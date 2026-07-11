from __future__ import annotations

import base64
import copy
import logging

from picasso.core.journal import JournalError
from picasso.session import session


logger = logging.getLogger(__name__)


def register(mcp) -> None:
    @mcp.tool()
    def list_journal_entries(
        limit: int = 50,
        status: str | None = None,
        tool: str | None = None,
        pass_name: str | None = None,
    ) -> dict:
        """List reverse-diff entries for the active world, newest first."""
        try:
            journal = session.require_journal()
            listing = journal.list_entries(
                limit=limit,
                status=status,
                tool=tool,
                pass_name=pass_name,
            )
            return {
                "ok": True,
                "journal_status": session.journal_status,
                **listing,
            }
        except JournalError as exc:
            return _journal_failure(exc)
        except ValueError as exc:
            return {"ok": False, "error": "invalid_argument", "message": str(exc)}
        except RuntimeError as exc:
            if str(exc) == "world_not_set":
                return _world_not_set()
            logger.exception("Unexpected error listing journal entries")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error listing journal entries")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def inspect_journal_entry(
        operation_id: str,
        change_offset: int = 0,
        change_limit: int = 100,
        include_artifact_content: bool = False,
    ) -> dict:
        """Inspect a paginated reverse diff without flooding the MCP context."""
        try:
            if change_offset < 0:
                raise ValueError("change_offset must be >= 0")
            if change_limit < 1 or change_limit > 500:
                raise ValueError("change_limit must be between 1 and 500")
            entry = session.require_journal().inspect(operation_id)
            changes = entry.get("changes", [])
            change_total = len(changes)
            page_end = min(change_offset + change_limit, change_total)
            entry["changes"] = changes[change_offset:page_end]
            entry["artifacts"] = _present_artifacts(
                entry.get("artifacts", []),
                include_content=include_artifact_content,
            )
            return {
                "ok": True,
                "journal_status": session.journal_status,
                "entry": entry,
                "change_total": change_total,
                "change_offset": change_offset,
                "change_limit": change_limit,
                "next_offset": page_end if page_end < change_total else None,
                "artifact_content_included": include_artifact_content,
            }
        except JournalError as exc:
            return _journal_failure(exc)
        except ValueError as exc:
            return {"ok": False, "error": "invalid_argument", "message": str(exc)}
        except RuntimeError as exc:
            if str(exc) == "world_not_set":
                return _world_not_set()
            logger.exception("Unexpected error inspecting a journal entry")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error inspecting a journal entry")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def revert_last_apply(operation_id: str | None = None) -> dict:
        """Revert the newest apply, skipping positions changed by third parties."""
        try:
            result = session.require_journal().revert_last(operation_id)
            # Even a partial revert changes the world snapshot; invalidating on
            # a conflict-only result is harmless and keeps this contract simple.
            session.last_region = None
            return {
                "ok": True,
                "journal_status": session.journal_status,
                **result,
                "summary": (
                    f"Reverted {result['changed']} blocks from operation "
                    f"{result['operation_id']}; skipped {result['skipped']} conflicts."
                ),
            }
        except JournalError as exc:
            return _journal_failure(exc)
        except RuntimeError as exc:
            if str(exc) == "world_not_set":
                return _world_not_set()
            logger.exception("Unexpected error reverting a journal entry")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error reverting a journal entry")
            return {"ok": False, "error": "internal_error", "message": str(exc)}


def _journal_failure(exc: JournalError) -> dict:
    session.note_journal_error(exc)
    return {
        "ok": False,
        "error": exc.code,
        "message": str(exc),
        "journal_status": session.journal_status,
        **exc.details,
    }


def _world_not_set() -> dict:
    return {
        "ok": False,
        "error": "world_not_set",
        "message": "Call set_world before using journal tools.",
        "journal_status": "unavailable",
    }


def _present_artifacts(
    artifacts: list[dict], *, include_content: bool
) -> list[dict]:
    presented = copy.deepcopy(artifacts)
    for artifact in presented:
        for key in ("before", "after"):
            snapshot = artifact.get(key)
            if not isinstance(snapshot, dict):
                continue
            content = snapshot.get("content_base64")
            if snapshot.get("exists") and isinstance(content, str):
                try:
                    snapshot["byte_count"] = len(base64.b64decode(content, validate=True))
                except Exception:
                    snapshot["byte_count"] = None
            else:
                snapshot["byte_count"] = 0
            if not include_content:
                snapshot.pop("content_base64", None)
    return presented
