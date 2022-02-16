package gt.edu.url;

import java.util.*;
import java.util.stream.Collectors;

public class CSP <V, D> {

    private List<V> variables;
    private Map<V, List<D>> originalDomains;
    private Map<V, List<D>> currentDomains;
    private Map<V, List<Constraint<V,D>>> constraints = new HashMap<>();
    private Queue<Arc<V, D>> inconsistencies = new ArrayDeque<>();

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
                if (!checkArcConsistency(unassigned, localAssignment)) return null;

                // Continue algorithm
                Map<V,D> result = backtrack(localAssignment, currentDomains);

                if (result != null) return result;
                this.currentDomains = currentDomains;
            }
        }
        return null;
    }

    private boolean checkArcConsistency(V currentVariable, Map<V,D> localAssignment) {
        var currentConstraints = this.constraints.get(currentVariable);
        for (V key: this.constraints.keySet()) {
            if (key != currentVariable) {
                var newConstraints = constraints.get(key).stream().filter(c -> currentConstraints.contains(c)).collect(Collectors.toList());
                for (var constraint: newConstraints) {
                    this.inconsistencies.add(new Arc<V, D>(key, currentVariable, constraint));
                }
            }
        }

        return AC3(localAssignment);
    }

    private boolean AC3(Map<V, D> localAssignment) {
        while(!this.inconsistencies.isEmpty()){
            var currentArc = this.inconsistencies.poll();
            if (Revise(currentArc.getOrigin(), currentArc, localAssignment)) {
                if (this.currentDomains.get(currentArc.getOrigin()).size() == 0) return false;

                var currentConstraints = this.constraints.get(currentArc.getOrigin());
                for (V key: this.constraints.keySet()) {
                    if (key != currentArc.getOrigin() && key != currentArc.getDestiny()) {
                        var newConstraints = constraints.get(key).stream().filter(c -> currentConstraints.contains(c)).collect(Collectors.toList());
                        for (var constraint: newConstraints) {
                            this.inconsistencies.add(new Arc<V, D>(key, currentArc.getOrigin(), constraint));
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean Revise(V fromVariable, Arc<V, D> arc, Map<V,D> localAssignment) {
        var isModified = false;
        var isValid = false;
        var newAssignment = new HashMap<>(localAssignment);

        for (var value: this.currentDomains.get(fromVariable)) {
            // existe algún valor en arc.destiny que puede satisfacer la constraint para el value
            isValid = false;

            for (var testValue: this.currentDomains.get(arc.getDestiny())) {
                //Verfiicar si combinación de value-testValue
                newAssignment.put(fromVariable, value);
                newAssignment.put(arc.getDestiny(), testValue);
                if (arc.getConstraint().satisfied(newAssignment)) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) { // No hay un valor en Dj que satisfaga la constraint con Di
                var newDomain = this.currentDomains.get(fromVariable).stream().filter(d -> d != value).collect(Collectors.toList());
                this.currentDomains.put(fromVariable, newDomain);
                isModified = true;
            }
        }
        return isModified;
    }
}
