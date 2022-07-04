package com.start.topic;


import org.springframework.beans.factory.annotation.Autowired;

public class TopicController {

	@Autowired
	TopicService topicService;
	TopicService ts;

	@Autowired
	TopicController(TopicService ts){
		this.ts = ts;
	}
}