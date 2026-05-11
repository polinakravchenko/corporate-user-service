package com.example.corporateusers.controller;

import com.example.corporateusers.dto.PasswordChangeForm;
import com.example.corporateusers.dto.StatusChangeForm;
import com.example.corporateusers.dto.UserCreateForm;
import com.example.corporateusers.dto.UserUpdateForm;
import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import com.example.corporateusers.exception.BusinessException;
import com.example.corporateusers.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String query,
                       @RequestParam(required = false) UserStatus status,
                       Model model) {
        model.addAttribute("users", userService.findUsers(query, status));
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("query", query);
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new UserCreateForm());
        model.addAttribute("statuses", UserStatus.values());
        return "users/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserCreateForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", UserStatus.values());
            return "users/create";
        }
        try {
            SystemUser user = userService.create(form);
            redirectAttributes.addFlashAttribute("success", "User was created: " + user.getUsername());
            return "redirect:/users/" + user.getId();
        } catch (BusinessException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("statuses", UserStatus.values());
            return "users/create";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        SystemUser user = userService.getUserWithPasswordHistory(id);
        model.addAttribute("user", user);
        model.addAttribute("passwordHistory", userService.getPasswordHistory(id));
        return "users/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SystemUser user = userService.getUser(id);
        UserUpdateForm form = new UserUpdateForm();
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        model.addAttribute("user", user);
        model.addAttribute("form", form);
        return "users/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserUpdateForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        SystemUser user = userService.getUser(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "users/edit";
        }
        try {
            userService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "User data was updated");
            return "redirect:/users/" + id;
        } catch (BusinessException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("user", user);
            return "users/edit";
        }
    }

    @GetMapping("/{id}/password")
    public String passwordForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getUser(id));
        model.addAttribute("form", new PasswordChangeForm());
        return "users/password";
    }

    @PostMapping("/{id}/password")
    public String changePassword(@PathVariable Long id,
                                 @Valid @ModelAttribute("form") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUser(id));
            return "users/password";
        }
        try {
            userService.changePassword(id, form);
            redirectAttributes.addFlashAttribute("success", "Password was changed and saved in history");
            return "redirect:/users/" + id;
        } catch (BusinessException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("user", userService.getUser(id));
            return "users/password";
        }
    }

    @GetMapping("/{id}/status")
    public String statusForm(@PathVariable Long id, Model model) {
        SystemUser user = userService.getUser(id);
        StatusChangeForm form = new StatusChangeForm();
        form.setStatus(user.getStatus());
        model.addAttribute("user", user);
        model.addAttribute("form", form);
        model.addAttribute("statuses", UserStatus.values());
        return "users/status";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @Valid @ModelAttribute("form") StatusChangeForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUser(id));
            model.addAttribute("statuses", UserStatus.values());
            return "users/status";
        }
        userService.changeStatus(id, form);
        redirectAttributes.addFlashAttribute("success", "User status was changed to " + form.getStatus());
        return "redirect:/users/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("success", "User was deleted");
        return "redirect:/users";
    }
}
