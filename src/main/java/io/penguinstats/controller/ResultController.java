package io.penguinstats.controller;

import io.penguinstats.model.DropMatrix;
import io.penguinstats.model.Item;
import io.penguinstats.model.Stage;
import io.penguinstats.model.Zone;
import io.penguinstats.service.*;
import io.penguinstats.util.CookieUtil;
import io.penguinstats.util.JSONUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/result")
public class ResultController {

	private static Logger logger = LogManager.getLogger(ResultController.class);

	@Autowired
	private ZoneService zoneService;
	@Autowired
	private StageService stageService;
	@Autowired
	private ItemService itemService;
	@Autowired
	private DropMatrixService dropMatrixService;
	@Autowired
	private ItemDropService itemDropService;
	@Autowired
	private CookieUtil cookieUtil;

	@GetMapping(path = "/stage/{stageId}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getResultForOneStage(@PathVariable("stageId") String stageId) {
		if (stageId == null)
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		JSONObject obj = generateReturnObjForOneStage(stageId, dropMatrixService.getAllElements());
		return new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
	}

	@PostMapping(path = "/stage/{stageId}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getPersonalResultForOneStage(HttpServletRequest request,
			@RequestBody String requestBody, @PathVariable("stageId") String stageId) {
		try {
			JSONObject obj = new JSONObject(requestBody);
			String userID = cookieUtil.readUserIDFromCookie(request);
			logger.info("user " + userID + " POST /stage/" + stageId + "\n" + obj.toString());

			JSONObject stageTimesObj = obj.has("stageTimes") ? obj.getJSONObject("stageTimes") : new JSONObject();
			JSONObject dropMatrixObj = obj.has("dropMatrix") ? obj.getJSONObject("dropMatrix") : new JSONObject();
			List<DropMatrix> elements =
					getDropMatrixListFromStageTimesAndDropMatrixMapObj(stageTimesObj, dropMatrixObj, userID);
			JSONObject returnObj = generateReturnObjForOneStage(stageId, elements);
			return new ResponseEntity<String>(returnObj.toString(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error in getPersonalResultForOneStage", e);
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(path = "/item/{itemId}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getResultForOneItem(@PathVariable("itemId") String itemId) {
		if (itemId == null)
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		JSONObject obj = generateReturnObjForOneItem(itemId, dropMatrixService.getAllElements());
		return new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
	}

	@PostMapping(path = "/item/{itemId}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getPersonalResultForOneItem(HttpServletRequest request,
			@RequestBody String requestBody, @PathVariable("itemId") String itemId) {
		try {
			JSONObject obj = new JSONObject(requestBody);
			String userID = cookieUtil.readUserIDFromCookie(request);
			logger.info("user " + userID + " POST /item/" + itemId + "\n" + obj.toString());

			JSONObject stageTimesObj = obj.has("stageTimes") ? obj.getJSONObject("stageTimes") : new JSONObject();
			JSONObject dropMatrixObj = obj.has("dropMatrix") ? obj.getJSONObject("dropMatrix") : new JSONObject();
			List<DropMatrix> elements =
					getDropMatrixListFromStageTimesAndDropMatrixMapObj(stageTimesObj, dropMatrixObj, userID);

			JSONObject returnObj = generateReturnObjForOneItem(itemId, elements);
			return new ResponseEntity<String>(returnObj.toString(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error in getPersonalResultForOneItem", e);
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@ApiOperation("Get matrix")
	@GetMapping(path = "/matrix", produces = "application/json;charset=UTF-8")
	public ResponseEntity<String> getMatrix(
			@RequestParam(name = "show_item_details", required = false, defaultValue = "false") boolean showItemDetails,
			@RequestParam(name = "show_stage_details", required = false,
					defaultValue = "false") boolean showStageDetails,
			@RequestParam(name = "show_closed_zones", required = false,
					defaultValue = "false") boolean showClosedZones) {
		try {
			logger.info("GET /matrix");
			JSONObject obj = new JSONObject();
			JSONArray array = new JSONArray();
			List<DropMatrix> elements = dropMatrixService.getAllElements();
			Map<String, Zone> zoneMap = showClosedZones ? null : zoneService.getZoneMap();
			Map<String, Item> itemMap = !showItemDetails ? null : itemService.getItemMap();
			Map<String, Stage> stageMap = !showStageDetails && showClosedZones ? null : stageService.getStageMap();

			for (DropMatrix element : elements) {
				JSONObject subObj = JSONUtil.convertObjectToJSONObject(element);
				if (!showClosedZones) {
					Stage stage = stageMap.get(element.getStageId());
					Zone zone = zoneMap.get(stage.getZoneId());
					Long currentTime = System.currentTimeMillis();
					if (zone.getOpenTime() != null && zone.getOpenTime().compareTo(currentTime) > 0
							|| zone.getCloseTime() != null && zone.getCloseTime().compareTo(currentTime) < 0) {
						continue;
					}
				}
				if (showItemDetails)
					subObj.put("item", JSONUtil.convertObjectToJSONObject(itemMap.get(element.getItemId())));
				if (showStageDetails)
					subObj.put("stage", JSONUtil.convertObjectToJSONObject(stageMap.get(element.getStageId())));
				array.put(subObj);
			}
			obj.put("matrix", array);
			return new ResponseEntity<String>(obj.toString(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error in getMatrix", e);
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private JSONObject generateReturnObjForOneStage(String stageId, List<DropMatrix> elements) {
		Map<String, Item> itemMap = itemService.getItemMap();
		Map<String, Stage> stageMap = stageService.getStageMap();
		JSONObject obj = new JSONObject();
		obj.put("stage", JSONUtil.convertObjectToJSONObject(stageMap.get(stageId)));
		JSONArray dropsArray = new JSONArray();
		for (DropMatrix dropMatrix : elements) {
			if (dropMatrix.getStageId().equals(stageId)) {
				JSONObject subObj =
						new JSONObject().put("quantity", dropMatrix.getQuantity()).put("times", dropMatrix.getTimes());
				Item item = itemMap.get(dropMatrix.getItemId());
				subObj.put("item", JSONUtil.convertObjectToJSONObject(item));
				dropsArray.put(subObj);
			}
		}
		obj.put("drops", dropsArray);
		return obj;
	}

	private JSONObject generateReturnObjForOneItem(String itemId, List<DropMatrix> elements) {
		Map<String, Item> itemMap = itemService.getItemMap();
		Map<String, Stage> stageMap = stageService.getStageMap();
		JSONObject obj = new JSONObject();
		obj.put("item", JSONUtil.convertObjectToJSONObject(itemMap.get(itemId)));
		JSONArray dropsArray = new JSONArray();
		for (DropMatrix dropMatrix : elements) {
			if (dropMatrix.getItemId().equals(itemId)) {
				JSONObject subObj =
						new JSONObject().put("quantity", dropMatrix.getQuantity()).put("times", dropMatrix.getTimes());
				Stage stage = stageMap.get(dropMatrix.getStageId());
				subObj.put("stage", JSONUtil.convertObjectToJSONObject(stage));
				dropsArray.put(subObj);
			}
		}
		obj.put("drops", dropsArray);
		return obj;
	}

	private List<DropMatrix> getDropMatrixListFromStageTimesAndDropMatrixMapObj(JSONObject stageTimesObj,
			JSONObject dropMatrixObj, String userID) {
		Criteria criteria = userID == null ? null : Criteria.where("userID").is(userID);
		Map<String, Map<String, DropMatrix>> matrixMapFromDB =
				userID != null ? itemDropService.generateDropMatrixMap(criteria) : new HashMap<>();
		Map<String, List<Integer>> stageTimesMapFromDB =
				userID != null ? itemDropService.getStageTimesMap(criteria) : new HashMap<>();
		Map<String, Item> itemMap = itemService.getItemMap();

		// merge quantity
		for (String stageId : dropMatrixObj.keySet()) {
			JSONObject subObj = dropMatrixObj.getJSONObject(stageId);
			Map<String, DropMatrix> subMap = matrixMapFromDB.getOrDefault(stageId, new HashMap<>());
			for (String itemId : subObj.keySet()) {
				Integer quantity = subObj.getInt(itemId);
				if (subMap.containsKey(itemId)) {
					DropMatrix dm = subMap.get(itemId);
					dm.setQuantity(dm.getQuantity() + quantity);
				} else {
					// set times to 0 temporarily, will update later
					subMap.put(itemId, new DropMatrix(stageId, itemId, quantity, 0));
				}
				matrixMapFromDB.put(stageId, subMap);
			}
		}

		// merge stage times
		for (String stageId : matrixMapFromDB.keySet()) {
			List<Integer> stageTimes = stageTimesMapFromDB.get(stageId);
			if (stageTimesObj.has(stageId)) {
				JSONArray stageTimesArray = stageTimesObj.getJSONArray(stageId);
				if (stageTimes == null) {
					stageTimes = new ArrayList<>();
					for (int i = 0; i < stageTimesArray.length(); i++) {
						stageTimes.add(stageTimesArray.getInt(i));
					}
				} else {
					if (stageTimes.size() < stageTimesArray.length()) {
						// something is wrong with stageTimesArray, skip this stage
						continue;
					}
					for (int i = 0; i < stageTimesArray.length(); i++) {
						stageTimes.set(i, stageTimes.get(i) + stageTimesArray.getInt(i));
					}
				}
			}
			stageTimesMapFromDB.put(stageId, stageTimes);
		}

		// update merged stage times into matrixMapFromDB
		for (String stageId : matrixMapFromDB.keySet()) {
			Map<String, DropMatrix> subMap = matrixMapFromDB.get(stageId);
			for (String itemId : subMap.keySet()) {
				Item item = itemMap.get(itemId);
				if (item == null)
					continue;
				Integer addTimePoint = item.getAddTimePoint();
				if (addTimePoint == null)
					addTimePoint = 0;
				Integer times = stageTimesMapFromDB.get(stageId).get(addTimePoint);
				subMap.get(itemId).setTimes(times);
			}
		}

		// convert matrixMapFromDB into a list of matrix elements
		List<DropMatrix> elements = new ArrayList<>();
		for (String stageId : matrixMapFromDB.keySet()) {
			for (String itemId : matrixMapFromDB.get(stageId).keySet()) {
				elements.add(matrixMapFromDB.get(stageId).get(itemId));
			}
		}
		return elements;
	}

}
