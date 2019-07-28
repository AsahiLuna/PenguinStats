package io.penguinstats.controller;

import io.penguinstats.model.Limitation;
import io.penguinstats.service.LimitationService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/limitations")
public class LimitationController {

	@Autowired
	private LimitationService limitationService;

	@ApiOperation("Get all real limitations")
	@GetMapping(produces = "application/json;charset=UTF-8")
	public ResponseEntity<List<Limitation>> getAllRealLimitations() {
		Map<String, Limitation> limitationsMap = limitationService.getRealLimitationMap();
		return new ResponseEntity<List<Limitation>>(new ArrayList<>(limitationsMap.values()), HttpStatus.OK);
	}

}
