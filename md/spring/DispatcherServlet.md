
<!-- TOC -->

- [springmvc核心类](#springmvc核心类)
    - [1、核心组件](#1核心组件)
    - [2、整个servlet的处理流程：](#2整个servlet的处理流程)
    - [3、doDispatch()核心逻辑](#3dodispatch核心逻辑)
    - [4、DispatcherServlet核心代码](#4dispatcherservlet核心代码)
        - [1、DispatcherType](#1dispatchertype)

<!-- /TOC -->

# springmvc核心类

## 1、核心组件

- 前端控制器（DispatcherServlet：类）：接收请求，响应结果，相当于电脑的CPU。
- 处理器映射器（HandlerMapping：接口）：根据URL去查找处理器
- 处理器（Handler：对象object）：（需要程序员去写代码处理逻辑的）
- 处理器适配器（HandlerAdapter：接口）：会把处理器包装成适配器，这样就可以支持多种类型的处理器，类比笔记本的适配器（适配器模式的应用）
- 视图解析器（ViewResovler：接口）：进行视图解析，多返回的字符串，进行处理，可以解析成对应的页面

## 2、整个servlet的处理流程：

service()--->processRequest()--->doservice()--->doDispatch()



## 3、doDispatch()核心逻辑

- 1、从HandlerMapping接口的实现类中通过request，找到:handler（HandlerExecutionChain类型：包含拦截器）
- 2、从HandlerAdapter接口通过上面handler找到适配器对象:ha
- 3、调用handler.applyPreHandle，实现调用拦截器栈中的所有前置拦截器
- 4、调用ha.handle处理具体的业务逻辑
- 5、调用handler.applyPostHandle，实现调用拦截器栈中的所有后置拦截器




## 4、DispatcherServlet核心代码
```java
public class DispatcherServlet extends FrameworkServlet {


@Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logRequest(request);

        // Keep a snapshot of the request attributes in case of an include,
        // to be able to restore the original attributes after the include.
        Map<String, Object> attributesSnapshot = null;
        if (WebUtils.isIncludeRequest(request)) {
            attributesSnapshot = new HashMap<>();
            Enumeration<?> attrNames = request.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = (String) attrNames.nextElement();
                if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                    attributesSnapshot.put(attrName, request.getAttribute(attrName));
                }
            }
        }

        // Make framework objects available to handlers and view objects.
        request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
        request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
        request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
        request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

        if (this.flashMapManager != null) {
            FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
            if (inputFlashMap != null) {
                request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
            }
            request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
            request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
        }

        try {
            doDispatch(request, response);   //核心方法
        }
        finally {
            if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
                // Restore the original attribute snapshot, in case of an include.
                if (attributesSnapshot != null) {
                    restoreAttributesAfterInclude(request, attributesSnapshot);
                }
            }
        }
    }


//主要的处理流程
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        boolean multipartRequestParsed = false;

        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

        try {
            ModelAndView mv = null;
            Exception dispatchException = null;

            try {
                processedRequest = checkMultipart(request);
                multipartRequestParsed = (processedRequest != request);

                // Determine handler for the current request.    拿到具体的controller中映射的方法
                mappedHandler = getHandler(processedRequest);
                if (mappedHandler == null) {
                    noHandlerFound(processedRequest, response);
                    return;
                }

                // Determine handler adapter for the current request.   对handler做一下适配
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

                // Process last-modified header, if supported by the handler.
                String method = request.getMethod();
                boolean isGet = "GET".equals(method);
                if (isGet || "HEAD".equals(method)) {
                    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                        return;
                    }
                }
                //拦截器的前置处理prehandle()
                if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                    return;
                }

                // Actually invoke the handler.   具体的业务处理
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }

                applyDefaultViewName(processedRequest, mv);
                //拦截器的前置处理posthandle()
                mappedHandler.applyPostHandle(processedRequest, response, mv);
            }
            catch (Exception ex) {
                dispatchException = ex;
            }
            catch (Throwable err) {
                // As of 4.3, we're processing Errors thrown from handler methods as well,
                // making them available for @ExceptionHandler methods and other scenarios.
                dispatchException = new NestedServletException("Handler dispatch failed", err);
            }
            processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
        }
        catch (Exception ex) {
            triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        }
        catch (Throwable err) {
            triggerAfterCompletion(processedRequest, response, mappedHandler,
                    new NestedServletException("Handler processing failed", err));
        }
        finally {
            if (asyncManager.isConcurrentHandlingStarted()) {
                // Instead of postHandle and afterCompletion
                if (mappedHandler != null) {
                    mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
                }
            }
            else {
                // Clean up any resources used by a multipart request.
                if (multipartRequestParsed) {
                    cleanupMultipart(processedRequest);
                }
            }
        }
    }



    //从HandlerMapping中获取handler
	@Nullable
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.handlerMappings != null) {
			for (HandlerMapping mapping : this.handlerMappings) {
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
		}
		return null;
	}


}
```


### 1、DispatcherType

HTTP请求的分发类型

```java
public enum DispatcherType {//请求转发的类型
    FORWARD,  
    INCLUDE,
    REQUEST,
    ASYNC,
    ERROR
}
```




有时一个请求需要多个Servlet协作才能完成，所以需要在一个Servlet跳到另一个Servlet！一个请求跨多个Servlet，需要使用转发和包含。

- 1、请求转发：由下一个Servlet完成响应体！当前Servlet可以设置响应头！（留头不留体）
- 2、请求包含：由两个Servlet共同完成响应体！（留头又留体）

无论是请求转发还是请求包含，都在一个请求范围内！使用同一个request和response！



> 请求转发和重定向的区别：
- 1、请求转发是一个请求一次响应，而重定向是两次请求两次响应。
- 2、请求转发地址不变化，而重定向会显示后一个请求的地址
- 3、请求转发只能转发到本项目其它Servlet，而重定向不只能重定向到本项目的其它Servlet，还能定向到其它项目
- 4、请求转发是服务端行为，只需给出转发的Servlet路径，而重定向需要给出requestURI，既包含项目名！

