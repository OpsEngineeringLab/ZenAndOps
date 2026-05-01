<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed; section>
    <#if section = "header">
        Sign In
    <#elseif section = "form">
        <div class="zenandops-subtitle">
            Enter your email and password to sign in!
        </div>
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="zenandops-form-group">
                    <label for="username" class="zenandops-label">
                        <#if !realm.loginWithEmailAllowed>Username<#elseif !realm.registrationEmailAsUsername>Email or Username<#else>Email</#if>
                        <span class="zenandops-required">*</span>
                    </label>
                    <input tabindex="1" id="username" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="off"
                           class="zenandops-input" placeholder="Enter your email"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                    <#if messagesPerField.existsError('username','password')>
                        <span class="zenandops-input-error" aria-live="polite">
                            ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </span>
                    </#if>
                </div>

                <div class="zenandops-form-group">
                    <label for="password" class="zenandops-label">
                        Password
                        <span class="zenandops-required">*</span>
                    </label>
                    <input tabindex="2" id="password" name="password" type="password" autocomplete="off"
                           class="zenandops-input" placeholder="Enter your password"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                </div>

                <div class="zenandops-form-options">
                    <#if realm.rememberMe && !usernameHidden??>
                        <div class="zenandops-checkbox-wrapper">
                            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                                   class="zenandops-checkbox"
                                   <#if login.rememberMe??>checked</#if> />
                            <label for="rememberMe" class="zenandops-checkbox-label">Keep me logged in</label>
                        </div>
                    </#if>
                </div>

                <div class="zenandops-form-buttons">
                    <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    <button tabindex="4" name="login" id="kc-login" type="submit" class="zenandops-btn-primary">
                        Sign in
                    </button>
                </div>
            </form>
        </#if>
    <#elseif section = "info">
    </#if>
</@layout.registrationLayout>
