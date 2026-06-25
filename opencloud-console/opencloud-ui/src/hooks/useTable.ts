import { ref, reactive } from 'vue'

export interface TableState<T = any> {
  loading: boolean
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface UseTableOptions<P extends Record<string, any>, T = any> {
  /** 分页查询函数，接收包含 pageNum/pageSize 的参数，返回 { records, total } */
  fetchFn: (params: P & { pageNum: number; pageSize: number }) => Promise<{ records: T[]; total: number }>
  /** 初始查询参数 */
  initialParams?: Partial<P>
  /** 默认每页条数 */
  defaultPageSize?: number
  /** 初始化时是否自动加载 */
  immediate?: boolean
}

/**
 * 通用分页表格 Hook
 */
export function useTable<P extends Record<string, any>, T = any>(options: UseTableOptions<P, T>) {
  const {
    fetchFn,
    initialParams = {} as Partial<P>,
    defaultPageSize = 20,
    immediate = true,
  } = options

  const state = reactive<TableState<T>>({
    loading: false,
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: defaultPageSize,
  })

  const queryParams = reactive<Record<string, any>>({ ...initialParams })

  async function fetchData() {
    state.loading = true
    try {
      const res = await fetchFn({
        ...queryParams,
        pageNum: state.pageNum,
        pageSize: state.pageSize,
      } as P & { pageNum: number; pageSize: number })
      state.list = res.records as any
      state.total = res.total
    } finally {
      state.loading = false
    }
  }

  function handleSearch() {
    state.pageNum = 1
    fetchData()
  }

  function handleReset(resetTo: Partial<P> = initialParams as Partial<P>) {
    Object.assign(queryParams, resetTo)
    handleSearch()
  }

  function handlePageChange(page: number) {
    state.pageNum = page
    fetchData()
  }

  function handleSizeChange(size: number) {
    state.pageSize = size
    state.pageNum = 1
    fetchData()
  }

  if (immediate) fetchData()

  return {
    state,
    queryParams,
    fetchData,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
  }
}
