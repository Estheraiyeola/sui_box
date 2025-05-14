package org.example.service;

import org.example.blockchain.SuiContractManager;
import org.example.models.generated.ModelRegistry;

public class ModelService {
    private final SuiContractManager mgr;

    public ModelService(SuiContractManager mgr) {
        this.mgr = mgr;
    }

    public Object instantiateModel(String structName, String objectId) {
        try {
            Class<?> modelClass = ModelRegistry.getModel(structName);
            return modelClass
                    .getConstructor(String.class, SuiContractManager.class)
                    .newInstance(objectId, mgr);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not instantiate " + structName + "Model", e);
        }
    }
}
