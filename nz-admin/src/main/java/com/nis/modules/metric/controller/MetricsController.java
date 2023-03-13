package com.nis.modules.metric.controller;

import com.nis.modules.sys.controller.AbstractController;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController extends AbstractController {

    /*@Autowired
    private NezhaMetrics nezhaMetrics;

    @Autowired
    private MeterRegistry meterRegistry;

    @RequestMapping(value = "/metrics", method = RequestMethod.GET)
    public ModelAndView counter1() {
        nezhaMetrics.bindTo(meterRegistry);
        ModelAndView mv = new ModelAndView();
        mv.setViewName("redirect:/prometheus");
        return mv;
    }*/
}
