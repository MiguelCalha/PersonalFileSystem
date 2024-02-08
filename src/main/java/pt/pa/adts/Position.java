package pt.pa.adts;

/**
 * An interface for a position, which is a holder object storing a
 * single element.
 * @author amfs
 */
public interface Position<E> {
  /** Return the element stored at this position. */
  E element();
}
