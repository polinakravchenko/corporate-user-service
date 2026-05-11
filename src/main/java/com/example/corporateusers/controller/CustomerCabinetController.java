package com.example.corporateusers.controller;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.exception.BusinessException;
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

    public CustomerCabinetController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String cabinet(Authentication authentication, Model model) {
        SystemUser user = userService.getByUsername(authentication.getName());
        model.addAttribute("user", user);
        return "cabinet/index";
    }

    @GetMapping("/edit")
    public String editProfileForm(Authentication authentication, Model model) {
        SystemUser user = userService.getByUsername(authentication.getName());
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
        SystemUser user = userService.getByUsername(authentication.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "cabinet/edit";
        }
        try {
            userService.updateOwnProfile(authentication.getName(), form);
            redirectAttributes.addFlashAttribute("success", "Дані профілю оновлено");
            return "redirect:/cabinet";
        } catch (BusinessException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("user", user);
            return "cabinet/edit";
        }
    }

    @GetMapping("/password")
    public String passwordForm(Authentication authentication, Model model) {
        model.addAttribute("user", userService.getByUsername(authentication.getName()));
        model.addAttribute("form", new PasswordChangeForm());
        return "cabinet/password";
    }

    @PostMapping("/password")
    public String changePassword(Authentication authentication,
                                 @Valid @ModelAttribute("form") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        SystemUser user = userService.getByUsername(authentication.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "cabinet/password";
        }
        try {
            userService.changeOwnPassword(authentication.getName(), form);
            redirectAttributes.addFlashAttribute("success", "Пароль успішно змінено");
            return "redirect:/cabinet";
        } catch (BusinessException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("user", user);
            return "cabinet/password";
        }
    }
}
