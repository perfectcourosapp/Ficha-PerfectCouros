package com.dmhrcompany.fichaperfectcouros;


import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.webkit.*;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.content.ContentValues;
import androidx.core.content.FileProvider;

import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    WebView web;
    ValueCallback<Uri[]> fileCallback;
    final int FILE_REQ=101;
    final int BACKUP_SAVE_REQ=201;
    final int BACKUP_OPEN_REQ=202;
    String pendingBackupJson=null;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        web=new WebView(this); setContentView(web);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p){
                if(fileCallback!=null) fileCallback.onReceiveValue(null);
                fileCallback=cb;
                try{ startActivityForResult(p.createIntent(),FILE_REQ); }
                catch(Exception e){ fileCallback=null; return false; }
                return true;
            }
        });
        web.addJavascriptInterface(new AndroidBridge(),"AndroidApp");
        web.loadUrl("file:///android_asset/index.html");
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int comma=dataUrl.indexOf(',');
        String raw=comma>=0?dataUrl.substring(comma+1):dataUrl;
        return Base64.getDecoder().decode(raw);
    }

    public class AndroidBridge {
        @JavascriptInterface public void saveBackupFile(String json){
            pendingBackupJson=json;
            runOnUiThread(() -> {
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE,"PerfectCouros_Backup.json");
                startActivityForResult(i,BACKUP_SAVE_REQ);
            });
        }

        @JavascriptInterface public void openBackupFile(){
            runOnUiThread(() -> {
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                String[] mime={"application/json","text/json","text/plain","application/octet-stream"};
                i.putExtra(Intent.EXTRA_MIME_TYPES,mime);
                startActivityForResult(i,BACKUP_OPEN_REQ);
            });
        }

        @JavascriptInterface public void closeApp(){
            runOnUiThread(() -> {
                web.clearFormData();
                web.clearHistory();
                finishAndRemoveTask();
            });
        }

        @JavascriptInterface public void shareImage(String dataUrl,String fileName){
            try{
                File dir=new File(getCacheDir(),"shared"); dir.mkdirs();
                File f=new File(dir,fileName);
                try(FileOutputStream out=new FileOutputStream(f)){ out.write(decodeDataUrl(dataUrl)); }
                Uri uri=FileProvider.getUriForFile(MainActivity.this,
                    getPackageName()+".fileprovider",f);
                Intent send=new Intent(Intent.ACTION_SEND);
                send.setType("image/png");
                send.putExtra(Intent.EXTRA_STREAM,uri);
                send.putExtra(Intent.EXTRA_TEXT,"Ficha de pedido Perfect Couros");
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(send,"Compartilhar ficha"));
            }catch(Exception e){ e.printStackTrace(); }
        }

        @JavascriptInterface public void saveImage(String dataUrl,String fileName){
            try{
                byte[] bytes=decodeDataUrl(dataUrl);
                ContentValues v=new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME,fileName);
                v.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
                if(Build.VERSION.SDK_INT>=29) v.put(MediaStore.Images.Media.RELATIVE_PATH,"Download/PerfectCouros");
                Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
                if(uri!=null) try(OutputStream out=getContentResolver().openOutputStream(uri)){ out.write(bytes); }
            }catch(Exception e){ e.printStackTrace(); }
        }

        @JavascriptInterface public void printImage(String dataUrl){
            runOnUiThread(() -> {
                WebView pw=new WebView(MainActivity.this);
                WebSettings ps=pw.getSettings(); ps.setJavaScriptEnabled(false);
                String page="<!doctype html><html><head><meta name='viewport' content='width=device-width'>"+
                    "<style>@page{size:A4 portrait;margin:0}html,body{margin:0;padding:0}img{display:block;width:100%;height:auto}</style>"+
                    "</head><body><img src='"+dataUrl+"'></body></html>";
                pw.setWebViewClient(new WebViewClient(){
                    @Override public void onPageFinished(WebView view,String url){
                        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);
                        PrintAttributes attrs=new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build();
                        pm.print("Ficha PerfectCouros",
                            view.createPrintDocumentAdapter("Ficha PerfectCouros"),attrs);
                    }
                });
                pw.loadDataWithBaseURL(null,page,"text/html","UTF-8",null);
            });
        }
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==FILE_REQ && fileCallback!=null){
            Uri[] r=WebChromeClient.FileChooserParams.parseResult(result,data);
            fileCallback.onReceiveValue(r); fileCallback=null;
            return;
        }
        if(req==BACKUP_SAVE_REQ){
            if(result==RESULT_OK && data!=null && data.getData()!=null && pendingBackupJson!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    out.write(pendingBackupJson.getBytes("UTF-8"));
                    web.post(() -> web.evaluateJavascript("alert(\'Backup salvo com sucesso.\')", null));
                }catch(Exception e){
                    web.post(() -> web.evaluateJavascript("alert(\'Erro ao salvar backup.\')", null));
                }
            }
            pendingBackupJson=null;
            return;
        }
        if(req==BACKUP_OPEN_REQ){
            if(result==RESULT_OK && data!=null && data.getData()!=null){
                try(InputStream in=getContentResolver().openInputStream(data.getData());
                    ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buf=new byte[8192]; int n;
                    while((n=in.read(buf))!=-1) out.write(buf,0,n);
                    String txt=out.toString("UTF-8");
                    final String encoded=android.util.Base64.encodeToString(txt.getBytes("UTF-8"),android.util.Base64.NO_WRAP);
                    web.evaluateJavascript("pcReceberBackupAndroid(decodeURIComponent(escape(atob('"+encoded+"'))))",null);
                }catch(Exception e){
                    web.post(() -> web.evaluateJavascript("alert(\'Erro ao abrir backup.\')", null));
                }
            }
            return;
        }
    }

    @Override public void onBackPressed(){
        if(web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
