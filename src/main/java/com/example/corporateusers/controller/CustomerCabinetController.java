package com.example.corporateusers.controller;

import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.exception.BusinessException;
import com.example.corporateusers.security.keycloak.KeycloakPrincipalExtractor;
import com.example.corporateusers.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cabinet")
public class CustomerCabinetController {

    private final UserService userService;
    private final KeycloakPrincipalExtractor principalExtractor;

    public CustomerCabinetController(UserService userService, KeycloakPrincipalExtractor principalExtractor) {
        this.userService = userService;
        this.principalExtractor = principalExtractor;
    }

    @GetMapping
    public String cabinet(Authentication authentication, Model model) {
        SystemUser user = currentUser(authentication);
        model.addAttribute("user", user);
        return "cabinet/index";
    }

    @GetMapping("/edit")
    public String editProfileForm(Authentication authentication, Model model) {
        SystemUser user = currentUser(authentication);
        UserUpdateForm form = new UserUpdateForm();
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        model.addAttribute("user", user);
        model.addAttribute("form", form);
        return "cabinet/edit";
    }

    @PostMapping("/edit")
    public String editProfile(Authentication authentication,
                              @Valid @ModelAttribute("form") UserUpdateForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        String username = principalExtractor.username(authentication);
        SystemUser user = userService.getByUsername(username);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "cabinet/edit";
        }
        try {
            userService.updateOwnProfile(username, form);
            redirectAttributes.addFlashAttribute("success", "Дані профілю оновлено");
            return "redirect:/cabinet";
        } catch (BusinessException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("user", user);
            return "cabinet/edit";
        }
    }

    private SystemUser currentUser(Authentication authentication) {
        return userService.getByUsername(principalExtractor.username(authentication));
    }
}
