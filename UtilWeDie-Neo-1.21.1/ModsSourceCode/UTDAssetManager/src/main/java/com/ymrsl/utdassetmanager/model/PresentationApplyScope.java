package com.ymrsl.utdassetmanager.model;

import com.google.gson.annotations.SerializedName;

/** Determines whether a presentation draft targets one exact asset identity or its whole registry id. */
public enum PresentationApplyScope {
    @SerializedName("registry")
    REGISTRY,

    @SerializedName("identity")
    IDENTITY
}
