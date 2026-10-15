package com.acme.salary.employee;

import java.net.URI;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	/** Only columns the UI sorts by; anything else would leak entity internals or fail at query time. */
	static final Set<String> SORTABLE_FIELDS = Set.of(
			"fullName", "employeeCode", "jobTitle", "department", "country", "annualSalary", "hireDate");

	private final EmployeeService service;

	public EmployeeController(EmployeeService service) {
		this.service = service;
	}

	@GetMapping
	public Page<EmployeeResponse> list(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Country country,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String jobTitle,
			@PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE_FIELDS.contains(order.getProperty())) {
				throw new InvalidSortException(order.getProperty(), SORTABLE_FIELDS);
			}
		}
		return service.list(new EmployeeFilter(search, country, department, jobTitle), pageable);
	}

	@GetMapping("/{id}")
	public EmployeeResponse get(@PathVariable long id) {
		return service.get(id);
	}

	@PostMapping
	public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
		EmployeeResponse created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public EmployeeResponse update(@PathVariable long id, @Valid @RequestBody UpdateEmployeeRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
