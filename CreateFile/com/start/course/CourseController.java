package com.start.course;


import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class CourseController {

	@Autowired
	List<com.start.topic.TopicController> topicController1;
	@Autowired
	CourseService courseService2;
	CourseService courseService;
	List<com.start.topic.TopicController> topicController;

	@Autowired
	CourseController(CourseService courseService,List<com.start.topic.TopicController> topicController){
		this.courseService = courseService;
		this.topicController = topicController;
	}
}