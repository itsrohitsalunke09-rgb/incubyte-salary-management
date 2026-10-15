package com.acme.salary.employee;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

	private final EmployeeRepository repository;

	public EmployeeService(EmployeeRepository repository) {
		this.repository = repository;
	}

	public Page<EmployeeResponse> list(EmployeeFilter filter, Pageable pageable) {
		return repository.findAll(filter.toSpecification(), pageable).map(EmployeeResponse::from);
	}

	public EmployeeResponse get(long id) {
		return EmployeeResponse.from(find(id));
	}

	@Transactional
	public EmployeeResponse create(CreateEmployeeRequest request) {
		String code = request.employeeCode().trim();
		// Friendly, explicit check first; the unique index still guards against concurrent creates.
		if (repository.existsByEmployeeCode(code)) {
			throw new DuplicateEmployeeCodeException(code);
		}
		try {
			return EmployeeResponse.from(repository.saveAndFlush(request.toEmployee()));
		}
		catch (DataIntegrityViolationException raceLost) {
			throw new DuplicateEmployeeCodeException(code);
		}
	}

	@Transactional
	public EmployeeResponse update(long id, UpdateEmployeeRequest request) {
		Employee employee = find(id);
		request.applyTo(employee);
		return EmployeeResponse.from(repository.saveAndFlush(employee));
	}

	@Transactional
	public void delete(long id) {
		repository.delete(find(id));
	}

	private Employee find(long id) {
		return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
	}

}
