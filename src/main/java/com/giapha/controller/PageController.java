package com.giapha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/trees";
    }

    @GetMapping("/trees")
    public String treeList(Model model) {
        return "tree-list";
    }

    @GetMapping("/trees/{id}/view")
    public String treeView(@PathVariable Long id, Model model) {
        model.addAttribute("treeId", id);
        return "tree-view";
    }
}
