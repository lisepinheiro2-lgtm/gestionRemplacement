package gestion.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class EmployeeAbsence {

    private String id = UUID.randomUUID().toString();
    private Employee employee;
    private EmployeeStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private final LocalTime startTime;
    private LocalTime endTime;

    public EmployeeAbsence(Employee employee, EmployeeStatus status, LocalDate startDate, LocalDate endDate) {
        this(employee, status, startDate, endDate, LocalTime.MIDNIGHT);
    }

    public EmployeeAbsence(Employee employee, EmployeeStatus status, LocalDate startDate, LocalDate endDate,
            LocalTime startTime) {
        this(employee, status, startDate, endDate, startTime, null);
    }

    public EmployeeAbsence(Employee employee, EmployeeStatus status, LocalDate startDate, LocalDate endDate,
            LocalTime startTime, LocalTime endTime) {
        this.employee = employee;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

}
