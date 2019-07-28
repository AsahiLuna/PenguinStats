package io.penguinstats.controller;

import io.penguinstats.model.Drop;
import io.penguinstats.model.ItemDrop;
import io.penguinstats.service.DropMatrixService;
import io.penguinstats.service.ItemDropService;
import io.penguinstats.service.UserService;
import io.penguinstats.util.CookieUtil;
import io.penguinstats.util.IpUtil;
import io.penguinstats.util.LimitationUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/report")
public class ReportController {

	private static Logger logger = LogManager.getLogger(ReportController.class);

	@Autowired
	private ItemDropService itemDropService;

	@Autowired
	private DropMatrixService dropMatrixService;

	@Autowired
	private UserService userService;

	@Autowired
	private LimitationUtil limitationUtil;

	@Autowired
	private CookieUtil cookieUtil;

	@ApiOperation("Save single report")
	@PostMapping
	public ResponseEntity<String> saveSingleReport(@RequestBody String requestBody, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			if (!isValidSingleReportRequest(requestBody)) {
				logger.warn("POST /report " + requestBody);
				return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
			}
			JSONObject obj = new JSONObject(requestBody);
			String userID = cookieUtil.readUserIDFromCookie(request);
			if (userID == null) {
				userID = userService.createNewUser(IpUtil.getIpAddr(request));
			}
			try {
				CookieUtil.setUserIDCookie(response, userID);
			} catch (UnsupportedEncodingException e) {
				logger.error("Error in handleUserIDFromCookie: ", e);
			}
			logger.info("user " + userID + " POST /report\n" + obj.toString(2));
			String ip = IpUtil.getIpAddr(request);
			String stageId = obj.getString("stageId");
			int furnitureNum = obj.getInt("furnitureNum");
			JSONArray dropsArray = obj.getJSONArray("drops");
			String source = obj.has("source") ? obj.getString("source") : null;
			String version = obj.has("version") ? obj.getString("version") : null;
			List<Drop> drops = new ArrayList<>();
			for (int i = 0; i < dropsArray.length(); i++) {
				JSONObject dropObj = dropsArray.getJSONObject(i);
				Drop drop = new Drop(dropObj.getString("itemId"), dropObj.getInt("quantity"));
				drops.add(drop);
			}
			if (furnitureNum > 0)
				drops.add(new Drop("furni", furnitureNum));
			Boolean isReliable = null;
			if (source != null && source.equals("penguin-stats.io(internal)"))
				isReliable = true;
			else
				isReliable = limitationUtil.checkDrops(drops, stageId);
			if (!isReliable)
				logger.warn("Abnormal drop data!");
			Long timestamp = System.currentTimeMillis();
			ItemDrop itemDrop = new ItemDrop(stageId, 1, drops, timestamp, ip, isReliable, source, version, userID);
			itemDropService.saveItemDrop(itemDrop);
			if (isReliable) {
				if (!dropMatrixService.hasElementsForOneStage(stageId))
					dropMatrixService.initializeElementsForOneStage(stageId);
				for (Drop drop : drops)
					dropMatrixService.increateQuantityForOneElement(stageId, drop.getItemId(), drop.getQuantity());
				dropMatrixService.increateTimesForOneStage(stageId, 1);
			}
			return new ResponseEntity<String>(HttpStatus.OK);
		} catch (JSONException jsonException) {
			logger.error("Error in saveSingleReport", jsonException);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			logger.error("Error in saveSingleReport", e);
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private boolean isValidSingleReportRequest(String jsonString) {
		try {
			JSONObject obj = new JSONObject(jsonString);
			if (!hasValidValue(obj, "stageId") || !hasValidValue(obj, "furnitureNum") || !hasValidValue(obj, "drops")) {
				return false;
			}
		} catch (JSONException e) {
			logger.error("Invalid single report request", e);
			return false;
		}
		return true;
	}

	private boolean hasValidValue(JSONObject obj, String key) {
		return obj.has(key) && !obj.isNull(key);
	}

}
