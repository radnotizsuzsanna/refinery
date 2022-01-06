/**
 */
package traffic.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import traffic.Car;
import traffic.Lane;
import traffic.TrafficPackage;
import traffic.TrafficSituation;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Situation</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link traffic.impl.TrafficSituationImpl#getLanes <em>Lanes</em>}</li>
 *   <li>{@link traffic.impl.TrafficSituationImpl#getCars <em>Cars</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TrafficSituationImpl extends MinimalEObjectImpl.Container implements TrafficSituation {
	/**
	 * The cached value of the '{@link #getLanes() <em>Lanes</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLanes()
	 * @generated
	 * @ordered
	 */
	protected EList<Lane> lanes;

	/**
	 * The cached value of the '{@link #getCars() <em>Cars</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCars()
	 * @generated
	 * @ordered
	 */
	protected EList<Car> cars;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TrafficSituationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TrafficPackage.Literals.TRAFFIC_SITUATION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Lane> getLanes() {
		if (lanes == null) {
			lanes = new EObjectContainmentEList<Lane>(Lane.class, this, TrafficPackage.TRAFFIC_SITUATION__LANES);
		}
		return lanes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Car> getCars() {
		if (cars == null) {
			cars = new EObjectContainmentEList<Car>(Car.class, this, TrafficPackage.TRAFFIC_SITUATION__CARS);
		}
		return cars;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case TrafficPackage.TRAFFIC_SITUATION__LANES:
				return ((InternalEList<?>)getLanes()).basicRemove(otherEnd, msgs);
			case TrafficPackage.TRAFFIC_SITUATION__CARS:
				return ((InternalEList<?>)getCars()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case TrafficPackage.TRAFFIC_SITUATION__LANES:
				return getLanes();
			case TrafficPackage.TRAFFIC_SITUATION__CARS:
				return getCars();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case TrafficPackage.TRAFFIC_SITUATION__LANES:
				getLanes().clear();
				getLanes().addAll((Collection<? extends Lane>)newValue);
				return;
			case TrafficPackage.TRAFFIC_SITUATION__CARS:
				getCars().clear();
				getCars().addAll((Collection<? extends Car>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case TrafficPackage.TRAFFIC_SITUATION__LANES:
				getLanes().clear();
				return;
			case TrafficPackage.TRAFFIC_SITUATION__CARS:
				getCars().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case TrafficPackage.TRAFFIC_SITUATION__LANES:
				return lanes != null && !lanes.isEmpty();
			case TrafficPackage.TRAFFIC_SITUATION__CARS:
				return cars != null && !cars.isEmpty();
		}
		return super.eIsSet(featureID);
	}

} //TrafficSituationImpl
