package blizkie.realdoor.profile;

/** Confirmation state of an extracted field. Only CONFIRMED or CORRECTED values may be reused downstream. */
public enum FieldStatus {
    UNCONFIRMED,
    CONFIRMED,
    CORRECTED
}
