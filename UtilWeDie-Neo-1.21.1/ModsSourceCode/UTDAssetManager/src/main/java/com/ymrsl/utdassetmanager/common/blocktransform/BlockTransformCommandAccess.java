package com.ymrsl.utdassetmanager.common.blocktransform;

/** Pure permission policy shared by command registration and unit tests. */
final class BlockTransformCommandAccess {
    private BlockTransformCommandAccess() {
    }

    static boolean managementAllowed(boolean hasPermissionLevelTwo, boolean singleplayerOwner) {
        return hasPermissionLevelTwo || singleplayerOwner;
    }
}
