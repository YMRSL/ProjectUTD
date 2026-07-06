package io.github.ymrsl.firstpersonfoodeating.client.animation;

public record AnimationProfile(
        String id,
        int idleVariants,
        int useVariants,
        float drawDuration,
        float inspectDuration,
        float putAwayDuration,
        float useDuration,
        float useEndDuration,
        float runStartDuration,
        float runEndDuration
) {
    public static AnimationProfile of(
            String id,
            int idleVariants,
            int useVariants,
            float drawDuration,
            float inspectDuration,
            float putAwayDuration,
            float useDuration,
            float useEndDuration,
            float runStartDuration,
            float runEndDuration
    ) {
        return new AnimationProfile(
                id,
                Math.max(idleVariants, 1),
                Math.max(useVariants, 1),
                drawDuration,
                inspectDuration,
                putAwayDuration,
                useDuration,
                useEndDuration,
                runStartDuration,
                runEndDuration
        );
    }
}
