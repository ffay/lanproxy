package org.fengfei.lanproxy.server.config.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *  * @author necho.duan
 *   * @title: URLHandler
 *    * @projectName lanproxy
 *     * @description:
 *      * @date 2021/1/11 16:29
 *       */
public class URLHelper {

    public static Set<String>  generateURLMap(Set<String> s, String rfloader, String folder){
          if(s==null){
              s=new HashSet<String>();
          }
          File file  = new File(folder);
          File[] t = file.listFiles();
          for(File sfile: t)
          {
              if(sfile.isFile()){
                s.add(sfile.getAbsolutePath().substring(sfile.getAbsolutePath().indexOf(rfloader)+rfloader.length()+1).replaceAll("\\\\","/"));
              }else{
                generateURLMap(s,rfloader,sfile.getAbsolutePath());
              }
          }
          return s;
    }


    public static void main(String[] args) {
        Set set =  generateURLMap(null,"D:\\github_code\\lanproxy\\proxy-server\\webpages","D:\\github_code\\lanproxy\\proxy-server\\webpages");
        System.out.println(Arrays.toString(set.toArray()));

    }


}
