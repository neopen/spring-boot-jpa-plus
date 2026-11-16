package io.github.neopen.demo.controller;

import io.github.neopen.demo.bean.entity.DocumentRecord;
import io.github.neopen.demo.service.DocumentFeatureService;
import io.github.neopen.demo.support.DemoContext;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

/**
 * 0.6 新增能力示例接口。
 *
 * <p>覆盖多租户隔离、行级数据权限、变更历史、逻辑删除恢复与
 * 批量 upsert。请求头 {@code X-Demo-Tenant} 指定租户，
 * {@code X-Demo-Dept} 指定部门。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@RestController
@RequestMapping("/feature/documents")
public class DocumentFeatureController {

    /** 文档能力示例服务。 */
    private final DocumentFeatureService documentFeatureService;

    /**
     * 创建示例控制器。
     *
     * @param documentFeatureService 文档能力示例服务
     */
    public DocumentFeatureController(
            DocumentFeatureService documentFeatureService
    ) {
        this.documentFeatureService = documentFeatureService;
    }

    /**
     * 查询当前租户与部门范围内的文档。
     *
     * @param tenant 租户标识
     * @param deptId 部门标识
     * @return 文档列表
     */
    @ApiOperation("按租户与部门查询文档")
    @SysControllerLog("按租户与部门查询文档")
    @GetMapping
    public Json<List<DocumentRecord>> list(
            @RequestHeader(
                    value = "X-Demo-Tenant",
                    required = false
            ) String tenant,
            @RequestHeader(
                    value = "X-Demo-Dept",
                    required = false
            ) Long deptId
    ) {
        return withContext(
                tenant,
                deptId,
                () -> Json.getSuccs(documentFeatureService.list())
        );
    }

    /**
     * 新增文档。
     *
     * @param tenant 租户标识
     * @param docNo  文档编号
     * @param title  标题
     * @param deptId 部门标识
     * @return 新增的文档
     */
    @ApiOperation("新增文档")
    @PostMapping
    public Json<DocumentRecord> create(
            @RequestHeader(
                    value = "X-Demo-Tenant",
                    required = false
            ) String tenant,
            @RequestParam String docNo,
            @RequestParam String title,
            @RequestParam(required = false) Long deptId
    ) {
        return withContext(
                tenant,
                deptId,
                () -> Json.getSucc(
                        documentFeatureService.create(docNo, title, deptId)
                )
        );
    }

    /**
     * 修改文档标题。
     *
     * @param tenant 租户标识
     * @param id     主键
     * @param title  新标题
     * @return 修改后的文档
     */
    @ApiOperation("修改文档标题并记录变更历史")
    @PostMapping("/{id}/rename")
    public Json<DocumentRecord> rename(
            @RequestHeader(
                    value = "X-Demo-Tenant",
                    required = false
            ) String tenant,
            @PathVariable Long id,
            @RequestParam String title
    ) {
        return withContext(
                tenant,
                null,
                () -> Json.getSucc(
                        documentFeatureService.rename(id, title)
                )
        );
    }

    /**
     * 逻辑删除文档。
     *
     * @param id 主键
     * @return 受影响记录数
     */
    @ApiOperation("逻辑删除文档")
    @DeleteMapping("/{id}")
    public Json<Integer> remove(@PathVariable Long id) {
        return Json.getSucc(documentFeatureService.remove(id));
    }

    /**
     * 恢复逻辑删除的文档。
     *
     * @param id 主键
     * @return 受影响记录数
     */
    @ApiOperation("恢复逻辑删除的文档")
    @PostMapping("/{id}/restore")
    public Json<Integer> restore(@PathVariable Long id) {
        return Json.getSucc(documentFeatureService.restore(id));
    }

    /**
     * 按文档编号批量新增或更新。
     *
     * @param tenant    租户标识
     * @param documents 文档列表
     * @return 新增与更新数量
     */
    @ApiOperation("按文档编号批量 upsert")
    @PostMapping("/upsert")
    public Json<Object> upsert(
            @RequestHeader(
                    value = "X-Demo-Tenant",
                    required = false
            ) String tenant,
            @RequestBody List<DocumentRecord> documents
    ) {
        return withContext(
                tenant,
                null,
                () -> Json.getSucc(
                        documentFeatureService.upsert(documents)
                )
        );
    }

    /**
     * 查看变更历史。
     *
     * @return 变更历史列表
     */
    @ApiOperation("查看变更历史")
    @GetMapping("/audit")
    public Json<Object> audit() {
        return Json.getSuccs(documentFeatureService.auditEntries());
    }

    /**
     * 设置演示上下文后执行操作。
     *
     * @param <T>    返回值类型
     * @param tenant 租户标识
     * @param deptId 部门标识
     * @param action 操作
     * @return 操作结果
     */
    private <T> T withContext(
            String tenant,
            Long deptId,
            Supplier<T> action
    ) {
        DemoContext.setTenant(
                tenant == null || tenant.isBlank()
                        ? DemoContext.DEFAULT_TENANT
                        : tenant
        );

        if (deptId != null) {
            DemoContext.setDeptId(deptId);
        }

        try {
            return action.get();
        } finally {
            DemoContext.clear();
        }
    }
}
