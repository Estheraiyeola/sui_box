package org.example;

import org.example.blockchain.SuiContractManager;
import org.example.models.generated.ModelRegistry;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws NoSuchMethodException {
        // Force the registry to initialize (optional; first .getModel() will do it too)
        Class<?> barModelClass = ModelRegistry.getModel("Bar");
        System.out.println("Found BarModel class: " + barModelClass);


        // ... continue bootstrapping your app ...
    }
}