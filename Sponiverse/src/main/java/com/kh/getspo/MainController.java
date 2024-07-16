package com.kh.getspo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonObject;

import dao.CategoryDAO;
import dao.EventDAO;
import dao.OrderDAO;
import dao.UserDAO;
import util.Common;
import vo.EventVO;
import vo.OrderVO;
import vo.UserVO;

@Controller
public class MainController {
	@Autowired
	UserDAO user_dao;
	@Autowired
	EventDAO event_dao;
	@Autowired
	CategoryDAO category_dao;
	@Autowired
	OrderDAO order_dao;

	@Autowired
	HttpSession session;

	@Autowired
	ServletContext app;

	public MainController(UserDAO user_dao, EventDAO event_dao, CategoryDAO category_dao, OrderDAO order_dao) {
		this.user_dao = user_dao;
		this.event_dao = event_dao;
		this.category_dao = category_dao;
		this.order_dao = order_dao;
	}

	// 메인페이지
	@RequestMapping(value = { "/", "main.do" })
	public String Main(Model model) {
		// 다가오는 행사 리스트
		List<EventVO> events = event_dao.fastevent();
		model.addAttribute("events", events);

		return Common.Main.VIEW_PATH + "main.jsp";
	}

	// 호스트페이지
	@RequestMapping("/hostMain.do")
	public String hostMain(Model model) {
		try {
			UserVO user = (UserVO) session.getAttribute("user");
			if (user != null) {
				List<EventVO> events = event_dao.selectEventByUser(user.getUser_idx());
				model.addAttribute("events", events);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return Common.Host.VIEW_PATH + "host.jsp";
	}

	// 7/9수정
	// 호스트이벤트관리
	@RequestMapping("/host_event_management.do")
	public String host_event_management(@RequestParam("event_idx") int event_idx, Model model) {

		try {
			// 해당 이벤트 정보 가져오기
			EventVO event = event_dao.eventByIdx(event_idx);
			model.addAttribute("event", event);
			
			int totaljoiner = event.getEvent_max_joiner();
			
			//행사 신청한 유저 전체 수 조회
			int appliecount = event_dao.applieCount(event_idx);
			model.addAttribute("appliecount", appliecount);
			
		    // 퍼센트 계산
		    double percent = 0;
		    if (totaljoiner > 0) {
		        percent = (double) appliecount / totaljoiner * 100;
		    }
		    
		    // 소수점 자릿수 포맷팅
		    String percentStr;
		    if (percent == (int) percent) {
		        percentStr = String.format("%d", (int) percent); // 정수 형태로
		    } else {
		        percentStr = String.format("%.2f", percent); // 소수점 2자리까지
		    }
		    
		    model.addAttribute("percent", percentStr);

			UserVO user = (UserVO) session.getAttribute("user");
			if (user != null) {
				List<EventVO> events = event_dao.selectEventByUser(user.getUser_idx());
				model.addAttribute("events", events);
			}
		} catch (Exception e) {
			// 예외 처리
			e.printStackTrace();
		}
		return Common.Host.VIEW_PATH + "host_event_management.jsp";
	}

	// 7/9수정
	// 호스트사이드바에서 이벤트 목록 조회 메소드
	@RequestMapping("/getEventList")
	public String getEventList(Model model) {
		// 내 행사 리스트
		List<EventVO> events = event_dao.fastevent();
		model.addAttribute("events", events);
		return Common.Host.VIEW_PATH + "host_sidebar.jsp";
	}

	// 7/4 수정
	// 호스트이벤트수정폼으로 이동
	@RequestMapping("/host_event_modify.do")
	public String host_event_modify(@RequestParam("event_idx") int event_idx, Model model) {
		EventVO event = event_dao.eventByIdx(event_idx);
		model.addAttribute("event", event);
		return Common.Host.VIEW_PATH + "host_event_modify.jsp";
	}

	@RequestMapping("/host_event_update.do")
	public String host_event_update(@ModelAttribute EventVO vo,
			@RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam("start_time") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
			@RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam("end_time") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
			@RequestParam("apply_start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applyStartDate,
			@RequestParam("apply_start_time") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime applyStartTime,
			@RequestParam("apply_end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applyEndDate,
			@RequestParam("apply_end_time") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime applyEndTime,
			@RequestParam("event_content") String event_content) {
		// 이벤트 시작 종료 시간 설정
		LocalDateTime eventHStart = LocalDateTime.of(startDate, startTime);
		LocalDateTime eventHEnd = LocalDateTime.of(endDate, endTime);
		LocalDateTime eventRStart = LocalDateTime.of(applyStartDate, applyStartTime);
		LocalDateTime eventREnd = LocalDateTime.of(applyEndDate, applyEndTime);

		vo.setEvent_h_start(eventHStart);
		vo.setEvent_h_end(eventHEnd);
		vo.setEvent_r_start(eventRStart);
		vo.setEvent_r_end(eventREnd);
		vo.setEvent_content(event_content);

		// 파일업로드 경로설정
		String webPath = "/resources/upload/";
		// upload까지의 절대경로
		String savePath = app.getRealPath(webPath);
		System.out.println(savePath);

		// 업로드 된 파일 정보
		MultipartFile photo = vo.getPhoto();

		String filename = "no_file";
		if (photo != null && !photo.isEmpty()) {
			filename = photo.getOriginalFilename();

			// 파일을 저장할 경로 생성
			File saveFile = new File(savePath, filename);

			if (!saveFile.exists()) {
				saveFile.mkdirs();
			} else {
				// 동일파일명이 존재하는 경우 업로드 시간을 추가하여 중복을 방지
				long time = System.currentTimeMillis();
				filename = String.format("%d_%s", time, filename);
				saveFile = new File(savePath, filename);
			}

			try {
				photo.transferTo(saveFile);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		vo.setEvent_thumbnail(filename);

		event_dao.updateEvent(vo);

		return "host_event_management.do";
	}

	// 썸머노트
	@RequestMapping(value = "/hostuploadSummernoteImageFile", produces = "application/json; charset=utf8")
	@ResponseBody
	public String uploadSummernoteImageFile(@RequestParam("file") MultipartFile multipartFile,
			HttpServletRequest request) {
		JsonObject jsonObject = new JsonObject();

		// 내부경로로 저장
		String contextRoot = new HttpServletRequestWrapper(request).getRealPath("/");
		String fileRoot = contextRoot + "resources/fileupload/";
		System.out.println("파일루트 경로: " + fileRoot);

		String originalFileName = multipartFile.getOriginalFilename(); // 오리지날 파일명
		String extension = originalFileName.substring(originalFileName.lastIndexOf(".")); // 파일 확장자
		String savedFileName = UUID.randomUUID() + extension; // 저장될 파일 명

		File targetFile = new File(fileRoot + savedFileName);
		try (InputStream fileStream = multipartFile.getInputStream()) {
			// 파일 저장
			FileUtils.copyInputStreamToFile(fileStream, targetFile);
			jsonObject.addProperty("url", request.getContextPath() + "/resources/fileupload/" + savedFileName);
			jsonObject.addProperty("responseCode", "success");
		} catch (IOException e) {
			// 저장된 파일 삭제
			FileUtils.deleteQuietly(targetFile);
			jsonObject.addProperty("responseCode", "error");
			e.printStackTrace();
		}

		return jsonObject.toString();
	}

	// 호스트페이지에서 참가자확인페이지 이동(0703 추가)
	@RequestMapping("/register_list.do")
	public String register_list(@RequestParam("event_idx") int event_idx, Model model) {
		
		//해당 이벤트 정보 가졍괴
		EventVO event = event_dao.eventByIdx(event_idx);
		model.addAttribute("event", event);
		
		
		//신청한 유저 전체 조회
		List<OrderVO> order = order_dao.orderByIdx(event_idx);
		model.addAttribute("order", order);
		
		//행사 신청한 유저 전체 수 조회
		int appliecount = event_dao.applieCount(event_idx);
		model.addAttribute("appliecount", appliecount);
		
		return Common.Host.VIEW_PATH + "host_register_list.jsp";
	}

	// 마이페이지이동(+수정을 위한 정보를 들고 가야함)
	@RequestMapping("/mypageform.do")
	public String mypage_form(Model model, int user_idx) {
		UserVO vo = user_dao.selectOne(user_idx);

		UserVO user = (UserVO) session.getAttribute("user");
		if (user != null) {
			List<OrderVO> order = order_dao.selectEventByorder(user.getUser_idx());
			model.addAttribute("order", order);
		}

		model.addAttribute("vo", vo);
		return Common.Mypage.VIEW_PATH + "mypage.jsp";
	}

	// 삭제 후에 보여질 페이지
	@RequestMapping("withdrawalform.do")
	public String withdrawalform() {
		return Common.Mypage.VIEW_PATH + "withdraw_after.jsp";
	}
	
	

}




























