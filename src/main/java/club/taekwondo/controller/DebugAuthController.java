package club.taekwondo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/debug")
public class DebugAuthController {
    @GetMapping("/whoami")
    public Map<String,Object> whoami(Authentication auth){
        Map<String,Object> m = new HashMap<>();
        if (auth == null){
            m.put("authenticated", false);
            return m;
        }
        m.put("authenticated", true);
        m.put("principal", auth.getPrincipal());
        m.put("authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        return m;
    }
}
