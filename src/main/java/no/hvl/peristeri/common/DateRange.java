package no.hvl.peristeri.common;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents a date range with a start and end date.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Embeddable
public class DateRange {

	private LocalDate startDate;
	private LocalDate endDate;

	/**
	 * Checks if this date range overlaps with another date range.
	 *
	 * @param other The other date range to check for overlap
	 * @return True if the date ranges overlap, false otherwise
	 */
	public boolean overlaps(DateRange other) {
		return startDate.isBefore(other.endDate) && endDate.isAfter(other.startDate);
	}

	/**
	 * Checks if this date range is before another date range.
	 *
	 * @param other The other date range to compare with
	 * @return True if this date range is before the other date range, false otherwise
	 */
	public boolean isBefore(DateRange other) {
		return endDate.isBefore(other.startDate);
	}

	/**
	 * Checks if this date range is after another date range.
	 *
	 * @param other The other date range to compare with
	 * @return True if this date range is after the other date range, false otherwise
	 */
	public boolean isAfter(DateRange other) {
		return startDate.isAfter(other.endDate);
	}

	/**
	 * Checks if a specific date is within this date range.
	 *
	 * @param date The date to check
	 * @return True if the date is within this date range, false otherwise
	 */
	public boolean isDuring(LocalDate date) {
		return !startDate.isAfter(date) && !endDate.isBefore(date);
	}

	/**
	 * Validates if the start date is before the end date.
	 *
	 * @return True if the start date is before the end date, false otherwise
	 */
	public boolean isValid() {
		return startDate.isBefore(endDate);
	}

	public String penString() {
		return startDate + " - " + endDate;
	}
}
