/**
 */
package traffic;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Lane</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link traffic.Lane#getFollowing <em>Following</em>}</li>
 *   <li>{@link traffic.Lane#getLeft <em>Left</em>}</li>
 *   <li>{@link traffic.Lane#getRight <em>Right</em>}</li>
 * </ul>
 *
 * @see traffic.TrafficPackage#getLane()
 * @model
 * @generated
 */
public interface Lane extends EObject {
	/**
	 * Returns the value of the '<em><b>Following</b></em>' reference list.
	 * The list contents are of type {@link traffic.Lane}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Following</em>' reference list.
	 * @see traffic.TrafficPackage#getLane_Following()
	 * @model
	 * @generated
	 */
	EList<Lane> getFollowing();

	/**
	 * Returns the value of the '<em><b>Left</b></em>' reference.
	 * It is bidirectional and its opposite is '{@link traffic.Lane#getRight <em>Right</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Left</em>' reference.
	 * @see #setLeft(Lane)
	 * @see traffic.TrafficPackage#getLane_Left()
	 * @see traffic.Lane#getRight
	 * @model opposite="right"
	 * @generated
	 */
	Lane getLeft();

	/**
	 * Sets the value of the '{@link traffic.Lane#getLeft <em>Left</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Left</em>' reference.
	 * @see #getLeft()
	 * @generated
	 */
	void setLeft(Lane value);

	/**
	 * Returns the value of the '<em><b>Right</b></em>' reference.
	 * It is bidirectional and its opposite is '{@link traffic.Lane#getLeft <em>Left</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Right</em>' reference.
	 * @see #setRight(Lane)
	 * @see traffic.TrafficPackage#getLane_Right()
	 * @see traffic.Lane#getLeft
	 * @model opposite="left"
	 * @generated
	 */
	Lane getRight();

	/**
	 * Sets the value of the '{@link traffic.Lane#getRight <em>Right</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Right</em>' reference.
	 * @see #getRight()
	 * @generated
	 */
	void setRight(Lane value);

} // Lane
