package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Planning {

	private ArrayList<WorkShift> workShifts = new ArrayList<>();

	private final String fileName = "workShifts.csv";
	private PersonnelList personnel;

	public Planning(PersonnelList personnel) {
		this.personnel = personnel;
		loadWorkShifts();
	}

	public void addWorkShift(Employee employee, String date, String startTime, String endTime) {
		workShifts.add(new WorkShift(employee, date, startTime, endTime));

		saveWorkShifts();
	}

	public void editWorkShift(int index, Employee employee, String date, String startTime, String endTime) {

		WorkShift workShift = workShifts.get(index);

		workShift.setEmployee(employee);
		workShift.setDate(date);
		workShift.setStartTime(startTime);
		workShift.setEndTime(endTime);

		saveWorkShifts();
	}

	private void saveWorkShifts() {

		try {

			FileWriter writer = new FileWriter(fileName);

			for (WorkShift workShift : workShifts) {

				writer.write(workShift.getEmployee().getFirstName() + ";" + workShift.getEmployee().getLastName() + ";"
						+ workShift.getDate() + ";" + workShift.getStartTime() + ";" + workShift.getEndTime()
						+ System.lineSeparator());
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde du fichier planning");
			e.printStackTrace();
		}
	}

	private Employee findEmployee(String firstName, String lastName) {

		for (Employee employee : personnel.getEmployees()) {

			if (employee.getFirstName().equals(firstName) && employee.getLastName().equals(lastName)) {

				return employee;
			}
		}

		return null;
	}

	private void loadWorkShifts() {

		File file = new File(fileName);
		if (!file.exists()) {
			return;
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] data = line.split(";");

				if (data.length == 5) {
					String firstName = data[0];
					String lastName = data[1];
					String date = data[2];
					String startTime = data[3];
					String endTime = data[4];
					Employee employee = findEmployee(firstName, lastName);

					if (employee != null) {
						workShifts.add(new WorkShift(employee, date, startTime, endTime));
					}

					workShifts.add(new WorkShift(employee, date, startTime, endTime));
				}

			}

			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur au chargement du fichier planning.");
			e.printStackTrace();
		}
	}

	public double getTotalHours(Employee employee) {

		double totalHours = 0;

		for (WorkShift workShift : workShifts) {

			if (workShift.getEmployee().getFirstName().equals(employee.getFirstName())
					&& workShift.getEmployee().getLastName().equals(employee.getLastName())) {

				totalHours += workShift.getDurationHours();
			}
		}

		return totalHours;
	}

	public void removeWorkShift(int index) {
		workShifts.remove(index);

		saveWorkShifts();
	}

	public ArrayList<WorkShift> getWorkShifts() {
		return workShifts;
	}

	public boolean isEmpty() {
		return workShifts.isEmpty();
	}

}
