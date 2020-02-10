package com.nis.modules.metric.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.nis.modules.metric.dto.NezhaMetrics;
import com.nis.modules.sys.controller.AbstractController;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@RestController
public class MetricsController extends AbstractController {
	@Autowired
	private NezhaMetrics nezhaMetrics;
	
    @RequestMapping(value = "/metrics", method= RequestMethod.GET)
    public ModelAndView counter1() {
    	nezhaMetrics.bindTo(new SimpleMeterRegistry());
    	ModelAndView mv = new ModelAndView();
    	mv.setViewName("redirect:/prometheus");
    	return mv;
    }

}