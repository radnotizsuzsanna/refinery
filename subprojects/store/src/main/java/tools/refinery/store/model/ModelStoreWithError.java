package tools.refinery.store.model;

import tools.refinery.store.representation.Symbol;

public class ModelStoreWithError {
	ModelStore modelStore = null;
	Exception exception = null;	//Has value of null if there was no error while reading the ModelStore from a file
	long lastSuccessfulTransactionVersion = -1; //Has value of -1 if there was no error while reading the ModelStore from a file
	Symbol guiltyRelation = null;	//Has value of null if there was no error while reading the ModelStore from a file

	public ModelStoreWithError(ModelStore modelStore, Exception exception, long lastSuccessfulTransactionVersion,
							   Symbol guiltyRelation){
		this.modelStore = modelStore;
		this.exception = exception;
		this.lastSuccessfulTransactionVersion = lastSuccessfulTransactionVersion;
		this.guiltyRelation = guiltyRelation;
	}

	public ModelStoreWithError(ModelStore modelStore){
		this.modelStore = modelStore;
	}

	public ModelStore getModelStore() {
		return modelStore;
	}

	public Exception getException() {
		return exception;
	}

	public Symbol getGuiltyRelation() {
		return guiltyRelation;
	}

	public long getLastSuccessfulTransactionVersion() {
		return lastSuccessfulTransactionVersion;
	}

	public void setModelStore(ModelStore modelStore) {
		this.modelStore = modelStore;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public void setGuiltyRelation(Symbol guiltyRelation) {
		this.guiltyRelation = guiltyRelation;
	}

	public void setLastSuccessfulTransactionVersion(long lastSuccessfulTransactionVersion) {
		this.lastSuccessfulTransactionVersion = lastSuccessfulTransactionVersion;
	}
}
