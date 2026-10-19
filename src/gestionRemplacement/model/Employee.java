package gestionRemplacement.model;

public class Employee {

    private String firstName;
    private String lastName;
    private double contractHours;
    private String statut;
    private double assignedHours;
    private double remainingHours;

    public Employee(String firstName, String lastName, double contractHours, String statut, double assignedHours,
            double remainingHours) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.contractHours = contractHours;
        this.statut = statut;
        this.assignedHours = assignedHours;
        this.remainingHours = remainingHours;
    }

    public String toString() {
        return firstName + " " + lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public double getContractHours() {
        return contractHours;
    }

    public void setContractHours(double contractHours) {
        this.contractHours = contractHours;
    }

    public double getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(double remainingHours) {
        this.remainingHours = remainingHours;
    }

    public double getAssignedHours() {
        return assignedHours;
    }

    public void setAssignedHours(double assignedHours) {
        this.assignedHours = assignedHours;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

}
