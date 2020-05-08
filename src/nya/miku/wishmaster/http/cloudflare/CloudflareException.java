/*
 * Overchan Android (Meta Imageboard Client)
 * Copyright (C) 2014-2016  miku-nyan <https://github.com/miku-nyan>
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nya.miku.wishmaster.http.cloudflare;

import nya.miku.wishmaster.api.HttpChanModule;
import nya.miku.wishmaster.api.interfaces.CancellableTask;
import nya.miku.wishmaster.common.MainApplication;
import nya.miku.wishmaster.http.interactive.InteractiveException;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;

/**
 * Исключение, вызванное запросом проверки Cloudflare
 * @author miku-nyan
 *
 */
public class CloudflareException extends InteractiveException {
    private static final long serialVersionUID = 1L;
    
    private static final String SERVICE_NAME = "Cloudflare";
    private static final String COOKIE_NAME = "cf_clearance";
    private static final String HCAPTCHA_KEY = "f9630567-8bfa-4fc9-8ee5-9c91c6276dff";
    
    private static final Pattern PATTERN_RTOKEN = Pattern.compile("name=\"r\" value=\"([^\"]*)\"");
    private static final Pattern PATTERN_ACTION = Pattern.compile("id=\"challenge-form\" action=\"([^\"]*)\"");
    
    private boolean hcaptcha;
    private String url;
    private String rToken;
    private String checkCaptchaUrl;
    private String chanName;
    
    //для создания экземплятов используются статические методы 
    private CloudflareException() {}
    
    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
    
    /**
     * Создать новый экземпляр cloudflare-исключения (обычная js-antiddos проверка, без капчи).
     * @param url адрес, по которому вызвана проверка
     * @param chanName название модуля чана (модуль должен имплементировать {@link HttpChanModule})
     * @return созданный объект
     */
    public static CloudflareException antiDDOS(String url, String chanName) {
        CloudflareException e = new CloudflareException();
        e.url = url;
        e.chanName = chanName;
        e.hcaptcha = false;
        return e;
    }
    
    @Deprecated
    public static CloudflareException antiDDOS(String url, String cookie, String chanName) {
        return antiDDOS(url, chanName);
    }
    
    public static CloudflareException withHcaptcha(String url, String chanName, String rToken, String checkCaptchaUrl) {
        CloudflareException e = new CloudflareException();
        e.url = url;
        e.hcaptcha = true;
        e.rToken = rToken;
        e.checkCaptchaUrl = checkCaptchaUrl;
        e.chanName = chanName;
        return e;
    }
    
    /**
     * Создать новый экземпляр cloudflare-исключения (проверка с капчей).
     * @param url адрес, по которому вызвана проверка
     * @param chanName название модуля чана
     * @param htmlString строка с html-страницей, загрузившейся с запросом проверки
     * @return созданный объект
     */
    public static CloudflareException withHcaptcha(String url, String chanName, String htmlString) {
        Matcher m;

        String action = null;
        m = PATTERN_ACTION.matcher(htmlString);
        if (m.find()) action = m.group(1);

        String rToken = null;
        m = PATTERN_RTOKEN.matcher(htmlString);
        if (m.find()) rToken = m.group(1);

        String checkCaptchaUrl = null;
        try {
            URL baseUrl = new URL(url);
            checkCaptchaUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + action;
        } catch (Exception e) {}
        return withHcaptcha(url, chanName, rToken, checkCaptchaUrl);
    }
    
    /**
     * определить тип проверки (с капчей или обычная anti-ddos)
     * @return true, если проверка с капчей
     */
    /*package*/ boolean isHcaptcha() {
        return hcaptcha;
    }
    
    /**
     * получить url, по которому была вызвана проверка
     * @return url
     */
    /*package*/ String getCheckUrl() {
        return url;
    }
    
    /**
     * получить открытый ключ капчи
     * @return открытый ключ
     */
    /*package*/ String getHcaptchaPublicKey() {
        return HCAPTCHA_KEY;
    }
    
    /**
     * получить значение параметра "r"
     * @return параметр "r"
     */
    /*package*/ String getRToken() {
        return rToken;
    }
    
    /**
     * получить URL для проверки капчи
     * @return строка, URL запроса
     */
    /*package*/ String getCheckCaptchaUrl() {
        return checkCaptchaUrl;
    }
    
    /**
     * получить название cloudflare-куки, которую необходимо получить
     * @return название cookie
     */
    /*package*/ String getRequiredCookieName() {
        return COOKIE_NAME;
    }
    
    @Override
    public void handle(Activity activity, CancellableTask task, Callback callback) {
        CloudflareUIHandler.handleCloudflare(this, (HttpChanModule) MainApplication.getInstance().getChanModule(chanName), activity, task, callback);
    }
}
