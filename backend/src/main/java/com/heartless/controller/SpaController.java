package com.heartless.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/{path:[^\\.]*}", "/{path1:[^\\.]*}/{path2:[^\\.]*}", "/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
