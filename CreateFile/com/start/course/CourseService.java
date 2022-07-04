package com.start.course;


import com.start.topic.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CourseService {

	@Autowired
	CourseRepository courseRepository;
	TopicRepository topicRepository;

	@Autowired
	CourseService(TopicRepository topicRepository){
		this.topicRepository = topicRepository;
	}
}