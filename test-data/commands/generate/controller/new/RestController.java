package {{root-package}}.{{feature}};

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class {{capitalizeFirst feature}}Controller {

	@GetMapping("/{{feature}}")
	public String greeting() {
		return "Hello {{feature}}";
	}
}