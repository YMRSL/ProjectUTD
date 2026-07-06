package io.github.ymrsl.firstpersonfoodeating.client.script.statemachine;

public class TrackArrayMismatchException extends RuntimeException {
    public TrackArrayMismatchException() {
        super("Track array does not match active controller.");
    }
}
