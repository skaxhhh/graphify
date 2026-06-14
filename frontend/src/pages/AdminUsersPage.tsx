import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAdminUsers, updateTradingAccess, createAdminUser } from "@/lib/adminApi";
import type { AdminUser } from "@/types/admin";

interface CreateUserForm {
  email: string;
  displayName: string;
  password: string;
  role: string;
}

const emptyForm: CreateUserForm = { email: "", displayName: "", password: "", role: "USER" };

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState<CreateUserForm>(emptyForm);

  const { data: users, isLoading, isError } = useQuery({
    queryKey: ["admin", "users", "list"],
    queryFn: async () => {
      const res = await fetchAdminUsers();
      return res.data ?? [];
    },
  });

  const toggleMutation = useMutation({
    mutationFn: ({ userId, enabled }: { userId: number; enabled: boolean }) =>
      updateTradingAccess(userId, enabled),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "users", "list"] });
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateUserForm) => createAdminUser(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "users", "list"] });
      setShowModal(false);
      setForm(emptyForm);
    },
  });

  if (isLoading) {
    return (
      <div className="mx-auto w-full max-w-[1200px]">
        <p className="text-sm text-muted-gray">불러오는 중...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto w-full max-w-[1200px]">
        <p className="text-sm text-red-500">사용자 목록을 불러오지 못했습니다.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-[1200px]">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-charcoal">유저 관리</h1>
          <p className="mt-1 text-sm text-muted-gray">트레이딩 봇 접근 권한을 관리합니다.</p>
        </div>
        <button
          type="button"
          onClick={() => setShowModal(true)}
          className="rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-btn-inset transition-opacity hover:opacity-90"
        >
          + 유저 추가
        </button>
      </header>

      <div className="overflow-hidden rounded-lg border border-warm-border bg-cream">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-warm-border bg-charcoal/[0.03]">
              <th className="px-4 py-3 text-left font-medium text-charcoal">이름</th>
              <th className="px-4 py-3 text-left font-medium text-charcoal">이메일</th>
              <th className="px-4 py-3 text-left font-medium text-charcoal">역할</th>
              <th className="px-4 py-3 text-left font-medium text-charcoal">가입일</th>
              <th className="px-4 py-3 text-center font-medium text-charcoal">트레이딩 접근</th>
            </tr>
          </thead>
          <tbody>
            {(users ?? []).map((user: AdminUser) => (
              <tr key={user.id} className="border-b border-warm-border last:border-0">
                <td className="px-4 py-3 text-charcoal">{user.displayName}</td>
                <td className="px-4 py-3 text-muted-gray">{user.email}</td>
                <td className="px-4 py-3">
                  <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${
                    user.role === "ADMIN"
                      ? "bg-charcoal/10 text-charcoal"
                      : "bg-charcoal/5 text-muted-gray"
                  }`}>
                    {user.role}
                  </span>
                </td>
                <td className="px-4 py-3 text-muted-gray">
                  {new Date(user.createdAt).toLocaleDateString("ko-KR")}
                </td>
                <td className="px-4 py-3 text-center">
                  <button
                    type="button"
                    onClick={() =>
                      toggleMutation.mutate({ userId: user.id, enabled: !user.tradingEnabled })
                    }
                    disabled={toggleMutation.isPending}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                      user.tradingEnabled ? "bg-charcoal" : "bg-charcoal/20"
                    }`}
                    aria-label={user.tradingEnabled ? "트레이딩 비활성화" : "트레이딩 활성화"}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        user.tradingEnabled ? "translate-x-6" : "translate-x-1"
                      }`}
                    />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {users?.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-muted-gray">
            등록된 사용자가 없습니다.
          </div>
        ) : null}
      </div>

      {/* 유저 추가 모달 */}
      {showModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <button
            type="button"
            className="absolute inset-0 bg-charcoal/30"
            aria-label="닫기"
            onClick={() => { setShowModal(false); setForm(emptyForm); }}
          />
          <div className="relative w-full max-w-md rounded-lg border border-warm-border bg-cream p-6 shadow-xl">
            <h2 className="mb-4 text-lg font-semibold text-charcoal">유저 추가</h2>

            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-xs font-medium text-charcoal">이메일</label>
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                  className="w-full rounded-md border border-warm-border bg-cream px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
                  placeholder="user@example.com"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-charcoal">이름</label>
                <input
                  type="text"
                  value={form.displayName}
                  onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
                  className="w-full rounded-md border border-warm-border bg-cream px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
                  placeholder="홍길동"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-charcoal">비밀번호</label>
                <input
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
                  className="w-full rounded-md border border-warm-border bg-cream px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
                  placeholder="••••••••"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-charcoal">역할</label>
                <select
                  value={form.role}
                  onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
                  className="w-full rounded-md border border-warm-border bg-cream px-3 py-2 text-sm text-charcoal focus:outline-none focus:ring-2 focus:ring-ring-blue"
                >
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
            </div>

            {createMutation.isError ? (
              <p className="mt-3 text-xs text-red-500">유저 생성에 실패했습니다.</p>
            ) : null}

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => { setShowModal(false); setForm(emptyForm); }}
                className="rounded-md border border-warm-border px-4 py-2 text-sm text-charcoal hover:opacity-80"
              >
                취소
              </button>
              <button
                type="button"
                disabled={createMutation.isPending || !form.email || !form.displayName || !form.password}
                onClick={() => createMutation.mutate(form)}
                className="rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-btn-inset transition-opacity hover:opacity-90 disabled:opacity-50"
              >
                {createMutation.isPending ? "생성 중..." : "생성"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
