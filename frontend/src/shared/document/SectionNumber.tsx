/**
 * Số câu hỏi hiển thị (yêu cầu 2026-11-10): phần số La Mã ở đầu (I., II.) viết bằng phông `roman` (Palatino, chữ hoa có
 * chân kiểu chữ khắc) để "I" không lẫn với chữ i hay l; đậm nhạt theo dòng chứa nó; phần sau (1., a., aa.) giữ phông của dòng.
 */
export function SectionNumber({ value }: { value: string }) {
  const match = /^([IVXLCDM]+)(\..*)$/.exec(value);
  if (!match) return <>{value}</>;
  return (
    <>
      <span className="font-roman tracking-[0.04em]">{match[1]}</span>
      {match[2]}
    </>
  );
}
