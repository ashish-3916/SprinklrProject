package com.start.topic;


import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TopicService {

	TopicRepository a;

	@Autowired
	TopicService(TopicRepository a){
		this.a = a;
	}
}