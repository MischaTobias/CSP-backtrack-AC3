package gt.edu.url;

import java.util.*;
import java.util.stream.Collectors;

public class CSP <V, D> {

    private List<V> variables;
    private Map<V, List<D>> originalDomains;
    private Map<V, List<D>> currentDomains;
    private Map<V, List<Constraint<V,D>>> constraints = new HashMap<>();

    public CSP(List<V> variables, Map<V, List<D>> domains){
        this.variables = variables;
        this.originalDomains = domains;

        for (V variable: variables) {
            constraints.put(variable, new ArrayList<Constraint<V, D>>());

            // Cada variable debe tener un dominio asignado
            if (!domains.containsKey(variable)){
                throw new IllegalArgumentException("La variable " + variable + "no contiene un dominio");
            }
        }
    }

    public void addConstraint(Constraint<V, D> constraint) {
        for (V variable: constraint.variables) {
            // Variable que se encuentra en el constraint también tiene que ser parte del CSP
            if (!this.variables.contains(variable)){
                throw new IllegalArgumentException("La variable " + variable + "No se encuentra en el CSP");
            }
            constraints.get(variable).add(constraint);
        }
    }

    public boolean consistent(V variable, Map<V, D> assignment) {
        for (Constraint<V, D> constraint: this.constraints.get(variable)){
            if (!constraint.satisfied(assignment)) {
                return false;
            }
        }
        return true;
    }

    public Map<V, D> backtrack() {
        return backtrack(new HashMap<>(), this.originalDomains);
    }

    private Map<V, D> backtrack(Map<V, D> assignment, Map<V, List<D>> currentDomains){
        // Si la asignación es completa (si cada variable tiene un valor)
        if (variables.size() == assignment.size()) return assignment;

        // Seleccionar una variable no asignada
        V unassigned = variables.stream()
                .filter(v -> !assignment.containsKey(v))
                .findFirst().get();

        this.currentDomains = currentDomains;

        for (D value: this.currentDomains.get(unassigned)) {
            System.out.println("Variable: " + unassigned + " valor: " + value);

            // Probar una asignación
            // 1 - Crear una copia de la asignación anterior
            Map<V,D> localAssignment = new HashMap<>(assignment);

            // 2 - Probar (asignar un valor)
            localAssignment.put(unassigned, value);
            // 3 - Verificar la consistencia de la asignación

            if (consistent(unassigned, localAssignment)) {
                List<D> newDomain = new ArrayList<>() {{
                    add(value);
                }};
                this.currentDomains.put(unassigned, newDomain);

                // Check arc-consistency
                if (!checkArcConsistency(unassigned)) return null;

                // Continue algorithm
                Map<V,D> result = backtrack(localAssignment, currentDomains);

                if (result != null) return result;
                this.currentDomains = currentDomains;
            }
        }
        return null;
    }

    private boolean checkArcConsistency(V changedVariable) {
        Queue<Arc<V, D>> inconsistencies = new ArrayDeque<>();
        for (Constraint<V, D> constraint: this.constraints.get(changedVariable)) {
            var inconsistencyOrigin = constraint.variables.stream().filter(v -> v != changedVariable).findFirst().get();
            inconsistencies.add(new Arc<V, D>(inconsistencyOrigin, changedVariable, constraint));
        }
        //for (V key: this.constraints.keySet()) {
        //    if (key != currentVariable) {
        //        var newConstraints = constraints.get(key).stream().filter(c -> currentConstraints.contains(c)).collect(Collectors.toList());
        //        for (var constraint: newConstraints) {
        //
        //        }
        //    }
        //}

        return AC3(inconsistencies);
    }

    private boolean AC3(Queue<Arc<V,D>> inconsistencies) {
        while(!inconsistencies.isEmpty()){
            var currentArc = inconsistencies.poll();
            if (Revise(currentArc)) {
                if (this.currentDomains.get(currentArc.getOrigin()).size() == 0) return false;

                for (Constraint<V, D> constraint: this.constraints.get(currentArc.getOrigin())) {
                    var inconsistencyOrigin = constraint.variables.stream().filter(v -> v != currentArc.getOrigin()).findFirst().get();
                    if (inconsistencyOrigin != currentArc.getDestiny()) {
                        inconsistencies.add(new Arc<V, D>(inconsistencyOrigin, currentArc.getOrigin(), constraint));
                    }
                }
            }
        }
        return true;
    }

    private boolean Revise(Arc<V, D> arc) {
        var isModified = false;
        var isValid = false;
        Map<V, D> newAssignment = new HashMap<>();

        for (var value: this.currentDomains.get(arc.getOrigin())) {
            // existe algún valor en arc.destiny que puede satisfacer la constraint para el value
            isValid = false;

            for (var testValue: this.currentDomains.get(arc.getDestiny())) {
                //Verfiicar si combinación de value-testValue
                newAssignment.put(arc.getOrigin(), value);
                newAssignment.put(arc.getDestiny(), testValue);
                if (arc.getConstraint().satisfied(newAssignment)) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) { // No hay un valor en Dj que satisfaga la constraint con Di
                var newDomain = this.currentDomains.get(arc.getOrigin()).stream().filter(d -> d != value).collect(Collectors.toList());
                this.currentDomains.put(arc.getOrigin(), newDomain);
                isModified = true;
            }
        }
        return isModified;
    }
}
