package io.penguinstats.controller;

import io.penguinstats.model.User;
import io.penguinstats.service.UserService;
import io.penguinstats.util.CookieUtil;
import io.penguinstats.util.IpUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

	public static final String INTERNAL_USER_ID_PREFIX = "internal_";

	private static Logger logger = LogManager.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@ApiOperation("Login")
	@PostMapping(produces = "text/plain;charset=UTF-8")
	public ResponseEntity<String> login(@RequestBody String userID, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			boolean isInternal = false;
			if (userID.startsWith(INTERNAL_USER_ID_PREFIX)) {
				isInternal = true;
				userID = userID.substring(INTERNAL_USER_ID_PREFIX.length());
			}
			User user = userService.getUserByUserID(userID);
			if (user == null) {
				if (isInternal) {
					userID = userService.createNewUser(userID, IpUtil.getIpAddr(request));
					if (userID == null) {
						logger.error("Failed to create new user.");
						return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
					} else {
						userService.addTag(userID, "internal");
					}
				} else {
					return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
				}
			}
			CookieUtil.setUserIDCookie(response, userID);
			return new ResponseEntity<String>(new JSONObject().put("userID", userID).toString(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error in getUser", e);
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
