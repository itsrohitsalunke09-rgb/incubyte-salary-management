package com.acme.salary.employee;

import java.util.Collection;
import java.util.TreeSet;

public class InvalidSortException extends RuntimeException {

	public InvalidSortException(String property, Collection<String> allowed) {
		super("Cannot sort by '" + property + "'. Allowed: " + String.join(", ", new TreeSet<>(allowed)));
	}

}
