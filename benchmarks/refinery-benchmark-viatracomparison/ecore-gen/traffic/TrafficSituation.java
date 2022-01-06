/**
 */
package traffic;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Situation</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link traffic.TrafficSituation#getLanes <em>Lanes</em>}</li>
 *   <li>{@link traffic.TrafficSituation#getCars <em>Cars</em>}</li>
 * </ul>
 *
 * @see traffic.TrafficPackage#getTrafficSituation()
 * @model
 * @generated
 */
public interface TrafficSituation extends EObject {
	/**
	 * Returns the value of the '<em><b>Lanes</b></em>' containment reference list.
	 * The list contents are of type {@link traffic.Lane}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Lanes</em>' containment reference list.
	 * @see traffic.TrafficPackage#getTrafficSituation_Lanes()
	 * @model containment="true"
	 * @generated
	 */
	EList<Lane> getLanes();

	/**
	 * Returns the value of the '<em><b>Cars</b></em>' containment reference list.
	 * The list contents are of type {@link traffic.Car}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Cars</em>' containment reference list.
	 * @see traffic.TrafficPackage#getTrafficSituation_Cars()
	 * @model containment="true"
	 * @generated
	 */
	EList<Car> getCars();

} // TrafficSituation
