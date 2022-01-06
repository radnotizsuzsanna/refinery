/**
 */
package traffic;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see traffic.TrafficFactory
 * @model kind="package"
 * @generated
 */
public interface TrafficPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "traffic";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "refinery-benchmark-viatracomparison.traffic";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "refinery-benchmark-viatracomparison.traffic";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	TrafficPackage eINSTANCE = traffic.impl.TrafficPackageImpl.init();

	/**
	 * The meta object id for the '{@link traffic.impl.TrafficSituationImpl <em>Situation</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see traffic.impl.TrafficSituationImpl
	 * @see traffic.impl.TrafficPackageImpl#getTrafficSituation()
	 * @generated
	 */
	int TRAFFIC_SITUATION = 0;

	/**
	 * The feature id for the '<em><b>Lanes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRAFFIC_SITUATION__LANES = 0;

	/**
	 * The feature id for the '<em><b>Cars</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRAFFIC_SITUATION__CARS = 1;

	/**
	 * The number of structural features of the '<em>Situation</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRAFFIC_SITUATION_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Situation</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRAFFIC_SITUATION_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link traffic.impl.LaneImpl <em>Lane</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see traffic.impl.LaneImpl
	 * @see traffic.impl.TrafficPackageImpl#getLane()
	 * @generated
	 */
	int LANE = 1;

	/**
	 * The feature id for the '<em><b>Following</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LANE__FOLLOWING = 0;

	/**
	 * The feature id for the '<em><b>Left</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LANE__LEFT = 1;

	/**
	 * The feature id for the '<em><b>Right</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LANE__RIGHT = 2;

	/**
	 * The number of structural features of the '<em>Lane</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LANE_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Lane</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LANE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link traffic.impl.CarImpl <em>Car</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see traffic.impl.CarImpl
	 * @see traffic.impl.TrafficPackageImpl#getCar()
	 * @generated
	 */
	int CAR = 2;

	/**
	 * The feature id for the '<em><b>On</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CAR__ON = 0;

	/**
	 * The number of structural features of the '<em>Car</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CAR_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Car</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CAR_OPERATION_COUNT = 0;


	/**
	 * Returns the meta object for class '{@link traffic.TrafficSituation <em>Situation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Situation</em>'.
	 * @see traffic.TrafficSituation
	 * @generated
	 */
	EClass getTrafficSituation();

	/**
	 * Returns the meta object for the containment reference list '{@link traffic.TrafficSituation#getLanes <em>Lanes</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Lanes</em>'.
	 * @see traffic.TrafficSituation#getLanes()
	 * @see #getTrafficSituation()
	 * @generated
	 */
	EReference getTrafficSituation_Lanes();

	/**
	 * Returns the meta object for the containment reference list '{@link traffic.TrafficSituation#getCars <em>Cars</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Cars</em>'.
	 * @see traffic.TrafficSituation#getCars()
	 * @see #getTrafficSituation()
	 * @generated
	 */
	EReference getTrafficSituation_Cars();

	/**
	 * Returns the meta object for class '{@link traffic.Lane <em>Lane</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Lane</em>'.
	 * @see traffic.Lane
	 * @generated
	 */
	EClass getLane();

	/**
	 * Returns the meta object for the reference list '{@link traffic.Lane#getFollowing <em>Following</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Following</em>'.
	 * @see traffic.Lane#getFollowing()
	 * @see #getLane()
	 * @generated
	 */
	EReference getLane_Following();

	/**
	 * Returns the meta object for the reference '{@link traffic.Lane#getLeft <em>Left</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Left</em>'.
	 * @see traffic.Lane#getLeft()
	 * @see #getLane()
	 * @generated
	 */
	EReference getLane_Left();

	/**
	 * Returns the meta object for the reference '{@link traffic.Lane#getRight <em>Right</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Right</em>'.
	 * @see traffic.Lane#getRight()
	 * @see #getLane()
	 * @generated
	 */
	EReference getLane_Right();

	/**
	 * Returns the meta object for class '{@link traffic.Car <em>Car</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Car</em>'.
	 * @see traffic.Car
	 * @generated
	 */
	EClass getCar();

	/**
	 * Returns the meta object for the reference '{@link traffic.Car#getOn <em>On</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>On</em>'.
	 * @see traffic.Car#getOn()
	 * @see #getCar()
	 * @generated
	 */
	EReference getCar_On();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	TrafficFactory getTrafficFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link traffic.impl.TrafficSituationImpl <em>Situation</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see traffic.impl.TrafficSituationImpl
		 * @see traffic.impl.TrafficPackageImpl#getTrafficSituation()
		 * @generated
		 */
		EClass TRAFFIC_SITUATION = eINSTANCE.getTrafficSituation();

		/**
		 * The meta object literal for the '<em><b>Lanes</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TRAFFIC_SITUATION__LANES = eINSTANCE.getTrafficSituation_Lanes();

		/**
		 * The meta object literal for the '<em><b>Cars</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TRAFFIC_SITUATION__CARS = eINSTANCE.getTrafficSituation_Cars();

		/**
		 * The meta object literal for the '{@link traffic.impl.LaneImpl <em>Lane</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see traffic.impl.LaneImpl
		 * @see traffic.impl.TrafficPackageImpl#getLane()
		 * @generated
		 */
		EClass LANE = eINSTANCE.getLane();

		/**
		 * The meta object literal for the '<em><b>Following</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LANE__FOLLOWING = eINSTANCE.getLane_Following();

		/**
		 * The meta object literal for the '<em><b>Left</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LANE__LEFT = eINSTANCE.getLane_Left();

		/**
		 * The meta object literal for the '<em><b>Right</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference LANE__RIGHT = eINSTANCE.getLane_Right();

		/**
		 * The meta object literal for the '{@link traffic.impl.CarImpl <em>Car</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see traffic.impl.CarImpl
		 * @see traffic.impl.TrafficPackageImpl#getCar()
		 * @generated
		 */
		EClass CAR = eINSTANCE.getCar();

		/**
		 * The meta object literal for the '<em><b>On</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CAR__ON = eINSTANCE.getCar_On();

	}

} //TrafficPackage
