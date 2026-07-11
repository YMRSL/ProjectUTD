from __future__ import annotations

from collections import Counter
from dataclasses import dataclass, field
from typing import Any

from picasso.config import config
from picasso.core.journal import JournalError, JournalUnavailableError
from picasso.core.style_engine import StyleEngine
from picasso.core.surface_classifier import classify_surfaces
from picasso.core.write_choke import WriteChoke
from picasso.models.region import RegionData


@dataclass
class BundleExecutionResult:
    ok: bool
    dry_run: bool
    passes_applied: list[dict[str, Any]] = field(default_factory=list)
    errors: list[dict[str, Any]] = field(default_factory=list)
    total_changed: int = 0
    placements_skipped: list[dict] = field(default_factory=list)
    region_mode_warning: str | None = None
    modded_write_warning: str | None = None


class BundleExecutor:
    def __init__(self, engine: StyleEngine, choke: WriteChoke, bridge=None, journal=None) -> None:
        self.engine = engine
        self.choke = choke
        self.bridge = bridge
        self.journal = journal

    def execute_region(
        self,
        bundle: dict,
        region: RegionData,
        *,
        seed: int | None,
        dry_run: bool,
        structure_type_filter: str | None,
        force_modded_write: bool = False,
        include_player_built: bool = False,
    ) -> BundleExecutionResult:
        effective_seed = int(seed if seed is not None else bundle.get("default_seed", 42))
        working_region = region.copy()
        result = BundleExecutionResult(
            ok=False,
            dry_run=dry_run,
            region_mode_warning=_region_mode_warning(bundle, structure_type_filter),
        )

        for entry in bundle.get("entries", []):
            structure_type = entry.get("structure_type")
            if structure_type_filter and structure_type != structure_type_filter:
                continue
            for pass_entry in entry.get("passes", []):
                pass_name = pass_entry["name"]
                intensity = float(pass_entry.get("intensity", 1.0))
                space_filter = pass_entry.get("space_filter")
                try:
                    style_pass = self.engine.pass_registry[pass_name]
                    raw_changes = self.engine.apply(
                        pass_name, working_region, intensity, effective_seed, space_filter
                    )
                    choke_result = self.choke.validate(
                        working_region,
                        raw_changes,
                        only_safe_blocks=style_pass.only_safe_blocks,
                        enforce_modded_gate=not dry_run,
                        force_modded_write=force_modded_write,
                        include_player_built=include_player_built,
                    )
                    if choke_result.blocked_error:
                        result.errors.append(
                            {
                                "pass": pass_name,
                                "error": choke_result.blocked_error,
                                "message": choke_result.blocked_message,
                            }
                        )
                        continue
                    changed = choke_result.changed_count
                    if not dry_run and self.bridge is not None:
                        if self.journal is None:
                            raise JournalUnavailableError(
                                "A durable journal is required before applying a bundle."
                            )
                        journal_result = self.journal.apply(
                            choke_result.changes,
                            tool="apply_bundle",
                            pass_name=pass_name,
                            seed=effective_seed,
                            argument_summary={
                                "bundle_name": bundle.get("name"),
                                "structure_type": structure_type,
                                "structure_type_filter": structure_type_filter,
                                "pass_name": pass_name,
                                "intensity": intensity,
                                "space_filter": space_filter,
                                "force_modded_write": force_modded_write,
                                "include_player_built": include_player_built,
                            },
                        )
                        changed = journal_result.changed
                    _merge_changes(working_region, choke_result.changes)
                    classify_surfaces(working_region)
                    if choke_result.modded_positions and not config.modded_write_verified:
                        result.modded_write_warning = (
                            "Forced non-vanilla write before Phase 1.5 verification."
                            if force_modded_write and not dry_run
                            else "Bundle includes non-vanilla blocks; dry_run=false requires verification or force."
                        )
                    result.total_changed += changed
                    result.placements_skipped.extend(choke_result.skipped)
                    pass_result = {
                        "structure_type": structure_type,
                        "pass": pass_name,
                        "intensity": intensity,
                        "space_filter": space_filter,
                        "changed" if not dry_run else "would_change": changed,
                    }
                    if not dry_run and self.bridge is not None:
                        pass_result["journal_entry"] = journal_result.reference()
                        pass_result["journal_warning"] = journal_result.warning
                    result.passes_applied.append(pass_result)
                except JournalError as exc:
                    result.errors.append(
                        {
                            "pass": pass_name,
                            "error": exc.code,
                            "message": str(exc),
                            **exc.details,
                        }
                    )
                except Exception as exc:
                    result.errors.append(
                        {"pass": pass_name, "error": "internal_error", "message": str(exc)}
                    )
        result.ok = bool(result.passes_applied)
        return result


def _merge_changes(region: RegionData, changes: RegionData) -> None:
    for pos, state in changes.blocks.items():
        if state.is_air:
            region.blocks.pop(pos, None)
        else:
            region.set(pos, state)
        region.surface_classes.pop(pos, None)
        region.space_classes.pop(pos, None)
        region.write_contexts.pop(pos, None)
        region.destructive_positions.discard(pos)
        region.block_entity_positions.discard(pos)
    if changes.blocks and region.atomic_groups:
        changed_positions = set(changes.blocks)
        region.atomic_groups = [
            remaining
            for group in region.atomic_groups
            if (remaining := group - changed_positions)
        ]


def _region_mode_warning(bundle: dict, structure_type_filter: str | None) -> str | None:
    if structure_type_filter:
        return None
    entries = bundle.get("entries", [])
    if not entries:
        return None
    pass_counts = Counter(
        pass_entry.get("name")
        for entry in entries
        for pass_entry in entry.get("passes", [])
        if pass_entry.get("name")
    )
    duplicates = {name: count for name, count in pass_counts.items() if count > 1}
    base = (
        "Region mode without structure_type_filter: all bundle entries run over the full "
        "region regardless of their structure_type labels."
    )
    if duplicates:
        return f"{base} Repeated passes: {duplicates}."
    return base
