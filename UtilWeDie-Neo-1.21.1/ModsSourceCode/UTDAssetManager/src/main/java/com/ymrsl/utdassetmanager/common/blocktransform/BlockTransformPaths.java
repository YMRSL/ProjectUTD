package com.ymrsl.utdassetmanager.common.blocktransform;

import java.nio.file.Path;

/** Fixed on-disk contract and non-sensitive command display paths. */
final class BlockTransformPaths {
    static final String ACTIVE_FILE_NAME = "block_transforms.json";
    static final String CANDIDATE_FILE_NAME = "block_transforms.candidate.json";
    static final String BACKUP_FILE_NAME = "block_transforms.json.bak";
    static final String ACTIVE_DISPLAY_PATH = "config/utd_asset_manager/" + ACTIVE_FILE_NAME;
    static final String CANDIDATE_DISPLAY_PATH = "config/utd_asset_manager/" + CANDIDATE_FILE_NAME;

    private BlockTransformPaths() {
    }

    static Path candidateFor(Path activePath) {
        return activePath.resolveSibling(CANDIDATE_FILE_NAME);
    }

    static Path backupFor(Path activePath) {
        return activePath.resolveSibling(BACKUP_FILE_NAME);
    }

    static String display(boolean candidate) {
        return candidate ? CANDIDATE_DISPLAY_PATH : ACTIVE_DISPLAY_PATH;
    }
}
