package oidc.controller;

import eu.olympus.client.interfaces.UserClient;
import eu.olympus.model.*;
import eu.olympus.model.exceptions.*;
import eu.olympus.model.server.rest.IdentityProof;
import oidc.model.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OidcController {
    private static final Logger logger = LoggerFactory.getLogger(OidcController.class);

    @Autowired
    UserClient userClient;

    @Autowired
    Policy policy;

    @Autowired
    Storage storage;



    // Login form
    @RequestMapping("/login")
    public String login(Model model, @RequestParam String redirect_uri, @RequestParam String state, @RequestParam String nonce, HttpServletRequest request) {
        request.getSession().setAttribute("redirectUrl", redirect_uri);
        request.getSession().setAttribute("state", state);
        request.getSession().setAttribute("nonce", nonce);
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        policy.setPolicyId(nonce);
        return "/login";
    }

    // Login form
    @RequestMapping("/loginFailed")
    public String login(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("loginError", true);
        return "/login";
    }

    @RequestMapping("/loginPage")
    public String loginPage(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", false);
        return "/login";
    }


    ////
    @PostMapping("/authenticate")
    public RedirectView authenticate(LoginRequest loginRequest, Model model, HttpServletRequest request) throws AuthenticationFailedException, TokenGenerationException {
        try {
            String token = userClient.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), policy, null, "NONE");
            model.addAttribute("username", loginRequest.getUsername());
            model.addAttribute("token", token);

            String redirectUrl = (String) request.getSession().getAttribute("redirectUrl");
            String state = (String) request.getSession().getAttribute("state");
//            storage.checkCredential();
//            System.out.println(storage.checkCredential());

            return new RedirectView(redirectUrl + "#state=" + state + "&id_token=" + token + "&token_type=bearer");
        } catch (Exception e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationFailedException.class) != -1) {


                return new RedirectView("/loginFailed", true);
            } else {
                throw e;
            }
        } finally {
            userClient.clearSession();
        }
    }

    @RequestMapping(value = "/redirectWithRedirectView", method = RequestMethod.GET)
    public RedirectView redirectWithUsingRedirectView(
            RedirectAttributes attributes, Model model, LoginRequest loginRequest) throws AuthenticationFailedException {
        // System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAa");
        // System.out.println(model.addAttribute("username", loginRequest.getUsername()));


        attributes.addFlashAttribute("flashmainAttribute", "redirectWithRedirectView");
        attributes.addAttribute("attribute", "redirectWithRedirectView");
        model.addAttribute("username", loginRequest.getUsername());

        return new RedirectView("/getpolicy");
    }

    @RequestMapping("/main")
//    @RequestMapping(value = "/main",method=RequestMethod.GET)
    public Object main(LoginRequest loginRequest, Model model, HttpServletRequest request) throws AuthenticationFailedException {

//        System.out.println(loginRequest.getUsername());

//       if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }
//          model.addAttribute("name", loginRequest.getUsername());


        model.addAttribute("username", loginRequest.getUsername());

        model.addAttribute("username", request.getSession().getAttribute("username"));

        // request.getSession().getAttribute("username");

        model.addAttribute("loginRequest", loginRequest);


        return "main";
    }


    // 3 bima  pairnei to policy pou exoume orisei
    @RequestMapping(value = "/getpolicy", method = RequestMethod.GET)
    public String policy(Model model, HttpServletRequest request, LoginRequest loginRequest) throws AuthenticationFailedException {
//        if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }
//        System.out.println(model.addAttribute("username", loginRequest.getUsername()));

//        System.out.println(policy);
//        System.out.println(loginRequest);


        ///
        policy.getPredicates().add(new Predicate("audience", Operation.REVEAL, new Attribute("olympus-service-provider")));
        storage.storeCredential(storage.getCredential());
        System.out.println(storage.getCredential());

        model.addAttribute("policy", request.getSession().getAttribute("policy"));
        model.addAttribute("checkCredential", request.getSession().getAttribute("checkCredential"));

        model.addAttribute("loginRequest", loginRequest);
        System.out.println(request.getSession());
        return "verify";
    }


    // Create User form
    @RequestMapping(value = "/createUser", method = RequestMethod.GET)
    public String createNewUser(Model model) {
        model.addAttribute("userExists", false);
        CreateUserRequest createUserRequest = new CreateUserRequest();
        model.addAttribute("createUserRequest", createUserRequest);
        return "/createUser";
    }

    @RequestMapping(value = "/createUser", method = RequestMethod.POST)
    public String postUser(@Valid @ModelAttribute("createUserRequest") CreateUserRequest createUserRequest, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "/createUser";
        }
        try {
            IdentityProof identityProof = constructIdentityProof(createUserRequest);
            userClient.createUserAndAddAttributes(createUserRequest.getUsername(), createUserRequest.getPassword(), identityProof);

        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, ExistingUserException.class) != -1) {
                model.addAttribute("userExists", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                model.addAttribute("userExists", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, UserCreationFailedException.class) != -1) {
                model.addAttribute("userExists", true);
            } else {
                model.addAttribute("unknownError", true);
            }
            //   logger.warn("Create user failed: " + exception);
            return "/createUser";
        }
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", true);
        userClient.clearSession();
        return "/login";
    }

    private AttributeIdentityProof constructIdentityProof(CreateUserRequest createUserRequest) {
        Map<String, Attribute> attributes = new HashMap<>();
        attributes.put("name", new Attribute(createUserRequest.getFirstName() + " " + createUserRequest.getLastName()));
        attributes.put("birthdate", new Attribute(createUserRequest.getBirthdate()));
        attributes.put("applicationcategory", new Attribute(createUserRequest.getApplicationcategory()));
        attributes.put("studentid", new Attribute(createUserRequest.getStudentid()));
        attributes.put("university", new Attribute(createUserRequest.getUniversity()));
        attributes.put("awardeddegree", new Attribute(createUserRequest.getAwardeddegree()));

        return new AttributeIdentityProof(attributes);
    }


    @RequestMapping("form")
    public String manageAccountPage(Model model, HttpServletRequest request) {
//        if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }

        model.addAttribute("applicationcategory", request.getSession().getAttribute("applicationcategory"));
        model.addAttribute("applicationentry", request.getSession().getAttribute("applicationentry"));
        model.addAttribute("academicprogramme", request.getSession().getAttribute("academicprogramme"));
        model.addAttribute("modestudy", request.getSession().getAttribute("modestudy"));
        model.addAttribute("proposedtopic", request.getSession().getAttribute("proposedtopic"));
        model.addAttribute("proposedsupervisor", request.getSession().getAttribute("proposedsupervisor"));
        model.addAttribute("proposedgroup", request.getSession().getAttribute("proposedgroup"));
        model.addAttribute("firstname", request.getSession().getAttribute("firstname"));
        model.addAttribute("lastname", request.getSession().getAttribute("lastname"));
        model.addAttribute("gender", request.getSession().getAttribute("gender"));
        model.addAttribute("email", request.getSession().getAttribute("email"));
        model.addAttribute("address1", request.getSession().getAttribute("address1"));
        model.addAttribute("address2", request.getSession().getAttribute("address2"));
        model.addAttribute("city", request.getSession().getAttribute("city"));
        model.addAttribute("country", request.getSession().getAttribute("country"));
        model.addAttribute("postalcode", request.getSession().getAttribute("postalcode"));
        model.addAttribute("departmentname", request.getSession().getAttribute("departmentname"));
        model.addAttribute("universityname", request.getSession().getAttribute("universityname"));
        model.addAttribute("organizationcountry", request.getSession().getAttribute("organizationcountry"));
        model.addAttribute("additionalinstitutions", request.getSession().getAttribute("additionalinstitutions"));
        model.addAttribute("schoolname", request.getSession().getAttribute("schoolname"));
        model.addAttribute("schoolCountry", request.getSession().getAttribute("schoolCountry"));


        return "/form";
    }

    @RequestMapping("form1")
    public String managePage(Model model, HttpServletRequest request) {
//        if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }

        model.addAttribute("applicationcategory", request.getSession().getAttribute("applicationcategory"));
        model.addAttribute("applicationentry", request.getSession().getAttribute("applicationentry"));
        model.addAttribute("academicprogramme", request.getSession().getAttribute("academicprogramme"));
        model.addAttribute("modestudy", request.getSession().getAttribute("modestudy"));
        model.addAttribute("proposedtopic", request.getSession().getAttribute("proposedtopic"));
        model.addAttribute("proposedsupervisor", request.getSession().getAttribute("proposedsupervisor"));
        model.addAttribute("proposedgroup", request.getSession().getAttribute("proposedgroup"));
        model.addAttribute("firstname", request.getSession().getAttribute("firstname"));
        model.addAttribute("lastname", request.getSession().getAttribute("lastname"));
        model.addAttribute("gender", request.getSession().getAttribute("gender"));
        model.addAttribute("email", request.getSession().getAttribute("email"));
        model.addAttribute("address1", request.getSession().getAttribute("address1"));
        model.addAttribute("address2", request.getSession().getAttribute("address2"));
        model.addAttribute("city", request.getSession().getAttribute("city"));
        model.addAttribute("country", request.getSession().getAttribute("country"));
        model.addAttribute("postalcode", request.getSession().getAttribute("postalcode"));
        model.addAttribute("departmentname", request.getSession().getAttribute("departmentname"));
        model.addAttribute("universityname", request.getSession().getAttribute("universityname"));
        model.addAttribute("organizationcountry", request.getSession().getAttribute("organizationcountry"));
        model.addAttribute("additionalinstitutions", request.getSession().getAttribute("additionalinstitutions"));
        model.addAttribute("schoolname", request.getSession().getAttribute("schoolname"));
        model.addAttribute("schoolCountry", request.getSession().getAttribute("schoolCountry"));


        return "/form1";
    }


    @RequestMapping("/changePassword")
    public String changePassword(Model model) {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        model.addAttribute("changePasswordRequest", changePasswordRequest);
        return "/changePassword";
    }


    @PostMapping("/changePassword")
    public String postChangePassword(@Valid ChangePasswordRequest changePasswordRequest, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "/changePassword";
        }
        try {
            userClient.changePassword(changePasswordRequest.getUsername(), changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword(), null, "NONE");
        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, UserCreationFailedException.class) != -1) {
                model.addAttribute("passwordChangeError", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                model.addAttribute("usernameWrongError", true);
            } else {
                model.addAttribute("unknownError", true);
            }
            return "/changePassword";
        }
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasChangedPassword", true);
        userClient.clearSession();
        return "/login";
    }


    @RequestMapping("/deleteAccount")
    public String deleteAccount(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        return "/deleteAccount";
    }


    @PostMapping("/deleteAccount")
    public String postDeleteAccount(LoginRequest loginRequest, Model model) {
        try {
            userClient.deleteAccount(loginRequest.getUsername(), loginRequest.getPassword(), null, "NONE");
        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                model.addAttribute("userDeletionError", true);
            } else {
                model.addAttribute("unknownError", true);
            }
            return "/deleteAccount";
        }
        loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasDeletedAccount", true);
        return "/login";
    }

    @RequestMapping("applicationform")
    public String changeAttributesPage(Model model, HttpServletRequest request) {
//        if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }


        ChangeAttributesRequest changeAttributesRequest = new ChangeAttributesRequest();
        String applicationcategory = (String) (request.getSession().getAttribute("applicationcategory"));
        String applicationentry = (String) (request.getSession().getAttribute("applicationentry"));
        String academicprogramme = (String) (request.getSession().getAttribute("academicprogramme"));
        String modestudy = (String) (request.getSession().getAttribute("modestudy"));
        String proposedtopic = (String) (request.getSession().getAttribute("proposedtopic"));
        String proposedsupervisor = (String) (request.getSession().getAttribute("proposedsupervisor"));
        String proposedgroup = (String) (request.getSession().getAttribute("proposedgroup;"));
        String firstname = (String) (request.getSession().getAttribute("firstname"));
        String lastname = (String) (request.getSession().getAttribute("lastname"));
        String gender = (String) (request.getSession().getAttribute("gender;"));
        String email = (String) (request.getSession().getAttribute("email"));
        String address1 = (String) (request.getSession().getAttribute("address1"));
        String address2 = (String) (request.getSession().getAttribute("address2"));
        String city = (String) (request.getSession().getAttribute("city"));
        String country = (String) (request.getSession().getAttribute("country"));
        String postalcode = (String) (request.getSession().getAttribute("postalcode"));
        String departmentname = (String) (request.getSession().getAttribute("departmentname"));
        String universityname = (String) (request.getSession().getAttribute("universityname"));
        String organizationcountry = (String) (request.getSession().getAttribute("organizationcountry"));
        String additionalinstitutions = (String) (request.getSession().getAttribute("additionalinstitutions"));
        String schoolname = (String) (request.getSession().getAttribute("schoolname"));
        String schoolCountry = (String) (request.getSession().getAttribute("schoolCountry"));

        changeAttributesRequest.setApplicationcategory(applicationcategory);
        changeAttributesRequest.setApplicationentry(applicationentry);
        changeAttributesRequest.setAcademicprogramme(academicprogramme);
        changeAttributesRequest.setModestudy(modestudy);
        changeAttributesRequest.setProposedtopic(proposedtopic);
        changeAttributesRequest.setProposedsupervisor(proposedsupervisor);
        changeAttributesRequest.setProposedgroup(proposedgroup);
        changeAttributesRequest.setFirstname(firstname);
        changeAttributesRequest.setLastname(lastname);
        changeAttributesRequest.setGender(gender);
        changeAttributesRequest.setEmail(email);
        changeAttributesRequest.setAddress1(address1);
        changeAttributesRequest.setAddress2(address2);
        changeAttributesRequest.setCity(city);
        changeAttributesRequest.setCountry(country);
        changeAttributesRequest.setPostalcode(postalcode);
        changeAttributesRequest.setDepartmentname(departmentname);
        changeAttributesRequest.setUniversityname(universityname);
        changeAttributesRequest.setOrganizationcountry(organizationcountry);
        changeAttributesRequest.setAdditionalinstitutions(additionalinstitutions);
        changeAttributesRequest.setSchoolname(schoolname);
        changeAttributesRequest.setSchoolCountry(schoolCountry);


        model.addAttribute("changeAttributesRequest", changeAttributesRequest);
        return "/applicationform";
    }


    @PostMapping("applicationform")
    public RedirectView postChangeAttributes(ChangeAttributesRequest changeAttributesRequest, BindingResult bindingResult, Model model, HttpServletRequest request, LoginRequest loginRequest) {
        IdentityProof identityProof = constructIdentityProof(changeAttributesRequest);
        if (bindingResult.hasErrors()) {
            return new RedirectView("/manageAccountPage", true);
        }
        try {

            userClient.createUserAndAddAttributes(changeAttributesRequest.getFirstname(), changeAttributesRequest.getLastname(), identityProof);
        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, ExistingUserException.class) != -1) {
                model.addAttribute("userExists", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                model.addAttribute("userExists", true);
            } else {
                model.addAttribute("unknownError", true);
            }
//            logger.warn("Create user failed: " + exception);
//            logger.warn(exception.getCause().toString());
            return new RedirectView("/applicationform", true);
        }

        model.addAttribute("applicationcategory", changeAttributesRequest.getApplicationcategory());
        model.addAttribute("applicationentry", changeAttributesRequest.getApplicationentry());
        model.addAttribute("academicprogramme", changeAttributesRequest.getAcademicprogramme());
        model.addAttribute("modestudy", changeAttributesRequest.getModestudy());
        model.addAttribute("proposedtopic", changeAttributesRequest.getProposedtopic());
        model.addAttribute("proposedsupervisor", changeAttributesRequest.getProposedsupervisor());
        model.addAttribute("proposedgroup", changeAttributesRequest.getProposedgroup());
        model.addAttribute("firstname", changeAttributesRequest.getFirstname());
        model.addAttribute("lastname", changeAttributesRequest.getLastname());
        model.addAttribute("gender", changeAttributesRequest.getGender());
        model.addAttribute("email", changeAttributesRequest.getEmail());
        model.addAttribute("address1", changeAttributesRequest.getAddress1());
        model.addAttribute("address2", changeAttributesRequest.getAddress2());
        model.addAttribute("city", changeAttributesRequest.getCity());
        model.addAttribute("country", changeAttributesRequest.getCountry());
        model.addAttribute("postalcode", changeAttributesRequest.getPostalcode());
        model.addAttribute("departmentname", changeAttributesRequest.getDepartmentname());
        model.addAttribute("universityname", changeAttributesRequest.getUniversityname());
        model.addAttribute("organizationcounty", changeAttributesRequest.getOrganizationcountry());
        model.addAttribute("additionalinstitutions", changeAttributesRequest.getAdditionalinstitutions());
        model.addAttribute("schoolname", changeAttributesRequest.getSchoolname());
        model.addAttribute("schoolCountry", changeAttributesRequest.getSchoolCountry());

        model.addAttribute("hasChanged", true);

        storage.storeCredential(storage.getCredential());
        System.out.println(storage.getCredential());

        request.getSession().setAttribute("applicationcategory", changeAttributesRequest.getApplicationcategory());
        request.getSession().setAttribute("applicationentry", changeAttributesRequest.getApplicationentry());

        request.getSession().setAttribute("academicprogramme", changeAttributesRequest.getAcademicprogramme());
        request.getSession().setAttribute("modestudy", changeAttributesRequest.getModestudy());
        request.getSession().setAttribute("proposedtopic", changeAttributesRequest.getProposedtopic());
        request.getSession().setAttribute("proposedsupervisor", changeAttributesRequest.getProposedsupervisor());
        request.getSession().setAttribute("proposedgroup", changeAttributesRequest.getProposedgroup());
        request.getSession().setAttribute("firstname", changeAttributesRequest.getFirstname());
        request.getSession().setAttribute("lastname", changeAttributesRequest.getLastname());
        request.getSession().setAttribute("gender", changeAttributesRequest.getGender());
        request.getSession().setAttribute("email", changeAttributesRequest.getEmail());
        request.getSession().setAttribute("address1", changeAttributesRequest.getAddress1());
        request.getSession().setAttribute("address2", changeAttributesRequest.getAddress2());
        request.getSession().setAttribute("city", changeAttributesRequest.getCity());
        request.getSession().setAttribute("country", changeAttributesRequest.getCountry());
        request.getSession().setAttribute("postalcode", changeAttributesRequest.getPostalcode());
        request.getSession().setAttribute("departmentname", changeAttributesRequest.getDepartmentname());
        request.getSession().setAttribute("universityname", changeAttributesRequest.getUniversityname());
        request.getSession().setAttribute("organizationcountry", changeAttributesRequest.getOrganizationcountry());
        request.getSession().setAttribute("additionalinstitutions", changeAttributesRequest.getAdditionalinstitutions());
        request.getSession().setAttribute("schoolname", changeAttributesRequest.getSchoolname());
        request.getSession().setAttribute("schoolCountry", changeAttributesRequest.getSchoolCountry());


        return new RedirectView("/form", true);
    }


    private AttributeIdentityProof constructIdentityProof(AttributeContainer createUserRequest) {
        Map<String, Attribute> attributes = new HashMap<>();

        attributes.put("applicationcategory", new Attribute(createUserRequest.getApplicationcategory()));
        attributes.put("academicprogramme", new Attribute(createUserRequest.getAcademicprogramme()));
        attributes.put("modestudy", new Attribute(createUserRequest.getModestudy()));
        attributes.put("proposedtopic", new Attribute(createUserRequest.getProposedtopic()));
        attributes.put("proposedsupervisor", new Attribute(createUserRequest.getProposedsupervisor()));
        attributes.put("proposedgroup", new Attribute(createUserRequest.getProposedgroup()));
        attributes.put("firstname", new Attribute(createUserRequest.getFirstname()));
        attributes.put("lastname", new Attribute(createUserRequest.getLastname()));
        attributes.put("gender", new Attribute(createUserRequest.getGender()));
        attributes.put("email", new Attribute(createUserRequest.getEmail()));
        attributes.put("address1", new Attribute(createUserRequest.getAddress1()));
        attributes.put("address2", new Attribute(createUserRequest.getAddress2()));
        attributes.put("city", new Attribute(createUserRequest.getCity()));
        attributes.put("country", new Attribute(createUserRequest.getCountry()));
        attributes.put("departmentname", new Attribute(createUserRequest.getDepartmentname()));
        attributes.put("universityname", new Attribute(createUserRequest.getUniversityname()));
        attributes.put("additionalinstitutions", new Attribute(createUserRequest.getAdditionalinstitutions()));
        attributes.put("schoolname", new Attribute(createUserRequest.getSchoolname()));
        attributes.put("schoolCountry", new Attribute(createUserRequest.getSchoolCountry()));


        return new AttributeIdentityProof(attributes);
    }


    ////
    private String getFrontpage(Model model) {

        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);

        return "/login";
    }

    @GetMapping("/verify")
    public String verify(Model model, HttpServletRequest request) {
//        if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }


        model.addAttribute("username", request.getSession().getAttribute("username"));
        model.addAttribute("policy", request.getSession().getAttribute("policy"));
//        model.addAttribute("country", request.getSession().getAttribute("country"));
//        model.addAttribute("departmentname", request.getSession().getAttribute("departmentname"));
//        model.addAttribute("universityname", request.getSession().getAttribute("universityname"));
//        model.addAttribute("organizationcountry", request.getSession().getAttribute("organizationcountry"));
//        model.addAttribute("additionalinstitutions", request.getSession().getAttribute("additionalinstitutions"));

        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        System.out.println(request.getSession());

        return "/verify";
    }

    @GetMapping("/storage")
    public String hello(Model model, HttpServletRequest request) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", false);
        return "/storage";
    }

    @PostMapping("/storage")
    public String showUserInfo(Model model, HttpServletRequest request, LoginRequest loginRequest) throws AuthenticationFailedException, OperationFailedException {
        String token = userClient.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), policy, null, "NONE");
        storage.checkCredential();
        System.out.println(storage.checkCredential());

        model.addAttribute("username", loginRequest.getUsername());
        model.addAttribute("token", token);
        model.addAttribute("firstname", request.getSession().getAttribute("firstname"));
        model.addAttribute("studentid", request.getSession().getAttribute("studentid"));
        model.addAttribute("university", request.getSession().getAttribute("university"));
        model.addAttribute("awardeddegree", request.getSession().getAttribute("awardeddegree"));
        model.addAttribute("loginRequest", loginRequest);

        return "storage";
    }


    public class SpringFileUploadController {

        @GetMapping("/index")
        public String hello() {
            return "applicationform";
        }

        @PostMapping("/upload")
        public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {

            String fileName = file.getOriginalFilename();
            try {
                file.transferTo(new File("C:\\upload\\" + fileName));

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            return ResponseEntity.ok("File uploaded successfully.");
        }


    }

    @RequestMapping("manageAccountLogin")
    public String manageAccountLogin(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        return "/manageAccountLogin";
    }

    @RequestMapping(value = "changeAttributes", method = RequestMethod.GET)
    public String changeAttributesPage1(Model model, HttpServletRequest request) {
//        if (request.getSession().getAttribute("loggedIn") == null || !(boolean) request.getSession().getAttribute("loggedIn")) {
//            return getFrontpage(model);
//        }
        Attributesreq attributesreq = new Attributesreq();
        String awardeddegree = (String) (request.getSession().getAttribute("awardeddegree"));
        String name = (String) (request.getSession().getAttribute("name"));
        attributesreq.setFirstname(name);


        attributesreq.setAwardeddegree(awardeddegree);
        model.addAttribute("attributesreq", attributesreq);
        return "/changeAttributes";
    }

    }








