package io.penguinstats.controller;

import io.penguinstats.model.Stage;
import io.penguinstats.service.StageService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stages")
public class StageController {

	@Autowired
	private StageService stageService;

	@ApiOperation("Get all stages")
	@GetMapping(produces = "application/json;charset=UTF-8")
	public ResponseEntity<List<Stage>> getAllStages(@RequestParam(value = "zoneId", required = false) String zoneId) {
		return new ResponseEntity<List<Stage>>(
				zoneId == null ? stageService.getAllStages() : stageService.getStagesByZoneId(zoneId), HttpStatus.OK);
	}

	@ApiOperation("Get stage by stage ID")
	@GetMapping(path = "/{stageId}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<Stage> getStageByStageId(@PathVariable("stageId") String stageId) {
		Stage stage = stageService.getStageByStageId(stageId);
		return new ResponseEntity<Stage>(stage, stage != null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
	}

}
